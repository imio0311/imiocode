package io.imiocode.agent;

import io.imiocode.config.AgentConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmException;
import io.imiocode.prompt.EnvironmentContextCollector;
import io.imiocode.prompt.EnvironmentContextProvider;
import io.imiocode.prompt.EnvironmentReminderFormatter;
import io.imiocode.tool.ToolExecution;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolSelection;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 驱动模型思考、工具执行和结果回传的 ReAct Agent。
 */
public final class Agent implements AutoCloseable {
    private final LlmClient client;
    private final ToolRegistry registry;
    private final AgentConfig config;
    private final StreamingTurnExecutor turnExecutor;
    private final int initialOutputTokenLimit;
    private final EnvironmentContextProvider environmentContextProvider;
    private final EnvironmentReminderFormatter environmentReminderFormatter;
    private final AtomicReference<ModeState> modeState =
            new AtomicReference<>(new ModeState(AgentMode.DO, false));
    private final AtomicReference<AgentTaskContext> activeTask = new AtomicReference<>();
    private final ScheduledExecutorService watchdog =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofVirtual().name("imio-agent-watchdog-", 0).factory());
    private final AtomicBoolean closed = new AtomicBoolean();

    public Agent(LlmClient client, ToolRegistry registry, AgentConfig config) {
        this(client, registry, config, 8_192);
    }

    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config,
            int initialOutputTokenLimit
    ) {
        this(
                client,
                registry,
                config,
                initialOutputTokenLimit,
                new EnvironmentContextCollector(
                        Path.of("").toAbsolutePath(),
                        Clock.systemDefaultZone(),
                        Duration.ofSeconds(2)),
                new EnvironmentReminderFormatter());
    }

    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config,
            int initialOutputTokenLimit,
            EnvironmentContextProvider environmentContextProvider,
            EnvironmentReminderFormatter environmentReminderFormatter
    ) {
        this.client = Objects.requireNonNull(client, "client 不能为空");
        this.registry = Objects.requireNonNull(registry, "registry 不能为空");
        this.config = Objects.requireNonNull(config, "config 不能为空");
        this.environmentContextProvider = Objects.requireNonNull(
                environmentContextProvider,
                "环境上下文提供器不能为空");
        this.environmentReminderFormatter = Objects.requireNonNull(
                environmentReminderFormatter,
                "环境提醒格式化器不能为空");
        if (initialOutputTokenLimit <= 0) {
            throw new IllegalArgumentException("initialOutputTokenLimit 必须为正数");
        }
        this.initialOutputTokenLimit = initialOutputTokenLimit;
        this.turnExecutor = new StreamingTurnExecutor(
                client, registry, config.maxParallelTools());
    }

    public AgentMode mode() {
        return modeState.get().mode();
    }

    public void switchMode(AgentMode nextMode, AgentEventListener listener) {
        Objects.requireNonNull(nextMode, "nextMode 不能为空");
        AgentEventListener checkedListener =
                Objects.requireNonNullElse(listener, AgentEventListener.NOOP);
        ensureOpen();
        while (true) {
            ModeState current = modeState.get();
            if (current.mode() == nextMode) {
                return;
            }
            boolean exitPending = current.mode() == AgentMode.PLAN
                    && nextMode == AgentMode.DO;
            ModeState updated = new ModeState(nextMode, exitPending);
            if (modeState.compareAndSet(current, updated)) {
                checkedListener.onEvent(new AgentEvent.ModeChanged(
                        current.mode(), nextMode));
                return;
            }
        }
    }

    public AgentResult run(AgentRequest request, AgentEventListener listener) {
        Objects.requireNonNull(request, "request 不能为空");
        AgentEventListener checkedListener =
                Objects.requireNonNullElse(listener, AgentEventListener.NOOP);
        ensureOpen();

        AgentTaskContext context = new AgentTaskContext(config.taskTimeout());
        if (!activeTask.compareAndSet(null, context)) {
            throw new IllegalStateException("同一时间只能运行一个 Agent 任务");
        }

        TaskModeSnapshot taskModeSnapshot = consumeTaskModeSnapshot();
        AgentMode taskMode = taskModeSnapshot.mode();
        ToolSelection selection = PlanModePrompt.toolSelection(taskMode);
        SystemReminder environmentReminder = environmentReminderFormatter.format(
                environmentContextProvider.capture());
        List<SystemReminder> sessionReminders = List.copyOf(request.reminders());
        List<ChatMessage> trajectory = new ArrayList<>();
        trajectory.add(request.userMessage());
        int iterations = 0;
        UnknownToolCircuitBreaker unknownTools = new UnknownToolCircuitBreaker();
        ScheduledFuture<?> timeoutFuture = watchdog.schedule(
                () -> context.requestStop(AgentStopReason.TIMEOUT, client),
                config.taskTimeout().toNanos(),
                TimeUnit.NANOSECONDS
        );

        try {
            checkedListener.onEvent(new AgentEvent.TaskStarted(taskMode));

            for (int iteration = 1; iteration <= config.maxIterations(); iteration++) {
                iterations = iteration;
                Optional<AgentStopReason> beforeIteration = context.stopReason();
                if (beforeIteration.isPresent()) {
                    return stopped(context, trajectory, iterations - 1,
                            beforeIteration.get(), checkedListener);
                }
                if (context.deadlineReached()) {
                    context.requestStop(AgentStopReason.TIMEOUT, client);
                    return stopped(context, trajectory, iterations - 1,
                            AgentStopReason.TIMEOUT, checkedListener);
                }

                checkedListener.onEvent(new AgentEvent.IterationStarted(iteration));
                List<SystemReminder> reminders = remindersForIteration(
                        environmentReminder,
                        sessionReminders,
                        taskMode,
                        taskModeSnapshot.includeExitReminder(),
                        iteration);
                StreamingTurnResult turn = turnExecutor.execute(
                        new ChatRequest(
                                requestMessages(request.committedHistory(), trajectory),
                                reminders,
                                selection,
                                OptionalInt.of(initialOutputTokenLimit)
                        ),
                        iteration,
                        iteration < config.maxIterations(),
                        context,
                        unknownTools,
                        checkedListener
                );
                ChatResponse response = turn.response();
                trajectory.add(response.message());

                Optional<AgentStopReason> afterModel = context.stopReason();
                if (afterModel.isPresent()) {
                    return stopped(context, trajectory, iteration,
                            afterModel.get(), checkedListener);
                }

                if (!response.hasToolCalls()) {
                    if (context.tryFinish(AgentStopReason.FINAL_RESPONSE)) {
                        emitSafely(
                                checkedListener,
                                new AgentEvent.TaskCompleted(iteration)
                        );
                        return AgentResult.completed(
                                trajectory,
                                response,
                                context.toolsExecuted(),
                                context.sideEffectsPossible()
                        );
                    }
                    return stopped(context, trajectory, iteration,
                            context.stopReason().orElse(AgentStopReason.CANCELLED),
                            checkedListener);
                }

                if (iteration == config.maxIterations()) {
                    context.tryFinish(AgentStopReason.MAX_ITERATIONS);
                    return stopped(context, trajectory, iteration,
                            AgentStopReason.MAX_ITERATIONS, checkedListener);
                }

                List<ToolExecution> executions = turn.toolExecutions();
                if (turn.toolsStarted()) {
                    context.markToolsExecuted();
                }

                Optional<AgentStopReason> afterTools = context.stopReason();
                if (afterTools.isPresent()) {
                    return stopped(context, trajectory, iteration,
                            afterTools.get(), checkedListener);
                }
                trajectory.add(toToolMessage(executions));
            }

            context.tryFinish(AgentStopReason.MAX_ITERATIONS);
            return stopped(context, trajectory, iterations,
                    AgentStopReason.MAX_ITERATIONS, checkedListener);
        } catch (UnknownToolCircuitOpenException exception) {
            if (!context.tryFinish(AgentStopReason.TOO_MANY_UNKNOWN_TOOLS)) {
                AgentStopReason existing = context.stopReason()
                        .orElse(AgentStopReason.TOO_MANY_UNKNOWN_TOOLS);
                return stopped(context, trajectory, iterations, existing, checkedListener);
            }
            return stopped(
                    context,
                    trajectory,
                    iterations,
                    AgentStopReason.TOO_MANY_UNKNOWN_TOOLS,
                    checkedListener);
        } catch (LlmException exception) {
            Optional<AgentStopReason> existing = context.stopReason();
            if (existing.isPresent() && existing.get() != AgentStopReason.ERROR) {
                return stopped(context, trajectory, iterations, existing.get(), checkedListener);
            }
            AgentError error = new AgentError(
                    exception.safeMessage(),
                    exception.recoverable(),
                    exception.retryAfter()
            );
            context.tryFinish(AgentStopReason.ERROR);
            return failed(context, trajectory, iterations, error, checkedListener);
        } catch (RuntimeException exception) {
            Optional<AgentStopReason> existing = context.stopReason();
            if (existing.isPresent() && existing.get() != AgentStopReason.ERROR) {
                return stopped(context, trajectory, iterations, existing.get(), checkedListener);
            }
            AgentError error = new AgentError("Agent 执行失败", false);
            context.tryFinish(AgentStopReason.ERROR);
            return failed(context, trajectory, iterations, error, checkedListener);
        } finally {
            timeoutFuture.cancel(false);
            activeTask.compareAndSet(context, null);
        }
    }

    private static List<SystemReminder> remindersForIteration(
            SystemReminder environmentReminder,
            List<SystemReminder> sessionReminders,
            AgentMode mode,
            boolean includeExitReminder,
            int iteration
    ) {
        List<SystemReminder> combined = new ArrayList<>(sessionReminders.size() + 2);
        combined.add(environmentReminder);
        combined.addAll(sessionReminders);
        PlanModePrompt.reminder(mode, iteration).ifPresent(combined::add);
        if (includeExitReminder && iteration == 1) {
            combined.add(PlanModePrompt.exitReminder());
        }
        return List.copyOf(combined);
    }

    private TaskModeSnapshot consumeTaskModeSnapshot() {
        while (true) {
            ModeState current = modeState.get();
            boolean includeExitReminder =
                    current.mode() == AgentMode.DO
                            && current.exitReminderPending();
            ModeState updated = includeExitReminder
                    ? new ModeState(current.mode(), false)
                    : current;
            if (updated == current || modeState.compareAndSet(current, updated)) {
                return new TaskModeSnapshot(
                        current.mode(),
                        includeExitReminder);
            }
        }
    }

    private static List<ChatMessage> requestMessages(
            List<ChatMessage> committed,
            List<ChatMessage> trajectory
    ) {
        List<ChatMessage> messages = new ArrayList<>(committed);
        messages.addAll(trajectory);
        return List.copyOf(messages);
    }

    private static ChatMessage toToolMessage(List<ToolExecution> executions) {
        if (executions.isEmpty()) {
            throw new IllegalStateException("工具调用未产生结果");
        }
        List<MessagePart> parts = executions.stream()
                .map(execution -> new ToolResultPart(
                        execution.call().id(),
                        execution.call().name(),
                        execution.result()
                ))
                .map(MessagePart.class::cast)
                .toList();
        return new ChatMessage(MessageRole.TOOL, parts);
    }

    private static AgentResult stopped(
            AgentTaskContext context,
            List<ChatMessage> trajectory,
            int iterations,
            AgentStopReason reason,
            AgentEventListener listener
    ) {
        emitSafely(listener, new AgentEvent.TaskStopped(
                reason, Math.max(iterations, 0), context.sideEffectsPossible()));
        return AgentResult.stopped(
                reason,
                trajectory,
                context.toolsExecuted(),
                context.sideEffectsPossible()
        );
    }

    private static AgentResult failed(
            AgentTaskContext context,
            List<ChatMessage> trajectory,
            int iterations,
            AgentError error,
            AgentEventListener listener
    ) {
        emitSafely(listener, new AgentEvent.TaskFailed(
                Math.max(iterations, 0), error, context.sideEffectsPossible()));
        return AgentResult.failed(
                trajectory,
                context.toolsExecuted(),
                context.sideEffectsPossible(),
                error
        );
    }

    public void cancelActive() {
        AgentTaskContext context = activeTask.get();
        if (context != null) {
            context.requestStop(AgentStopReason.CANCELLED, client);
        }
    }

    private static void emitSafely(AgentEventListener listener, AgentEvent event) {
        try {
            listener.onEvent(event);
        } catch (RuntimeException ignored) {
            // 终态已经确定，UI 异常不能改变结果或触发第二个终态。
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Agent 已关闭");
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            cancelActive();
            watchdog.shutdownNow();
            client.close();
        }
    }

    private record ModeState(
            AgentMode mode,
            boolean exitReminderPending
    ) {
        private ModeState {
            Objects.requireNonNull(mode, "mode 不能为空");
        }
    }

    private record TaskModeSnapshot(
            AgentMode mode,
            boolean includeExitReminder
    ) {
        private TaskModeSnapshot {
            Objects.requireNonNull(mode, "mode 不能为空");
        }
    }
}
