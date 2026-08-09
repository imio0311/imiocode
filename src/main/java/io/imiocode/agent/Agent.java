package io.imiocode.agent;

import io.imiocode.config.AgentConfig;
import io.imiocode.context.AutoCompactTrackingState;
import io.imiocode.context.ContextManageMode;
import io.imiocode.context.ContextManager;
import io.imiocode.context.ContextRequest;
import io.imiocode.context.ContextResult;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.hook.HookEvent;
import io.imiocode.hook.HookContext;
import io.imiocode.hook.HookRuntime;
import io.imiocode.hook.integration.HookContextFactory;
import io.imiocode.prompt.EnvironmentContextCollector;
import io.imiocode.prompt.EnvironmentContextProvider;
import io.imiocode.prompt.EnvironmentReminderFormatter;
import io.imiocode.permission.PermissionGate;
import io.imiocode.permission.PermissionReply;
import io.imiocode.skill.SkillActivator;
import io.imiocode.skill.SkillRunScope;
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
    private final PermissionGate permissionGate;
    private final ContextManager contextManager;
    private final SkillActivator skillActivator;
    private final boolean closeSharedResources;
    private final HookRuntime hooks;
    private final HookContextFactory hookContexts;
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
                new EnvironmentReminderFormatter(),
                null,
                null);
    }

    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config,
            int initialOutputTokenLimit,
            EnvironmentContextProvider environmentContextProvider,
            EnvironmentReminderFormatter environmentReminderFormatter
    ) {
        this(
                client,
                registry,
                config,
                initialOutputTokenLimit,
                environmentContextProvider,
                environmentReminderFormatter,
                null,
                null);
    }

    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config,
            int initialOutputTokenLimit,
            EnvironmentContextProvider environmentContextProvider,
            EnvironmentReminderFormatter environmentReminderFormatter,
            PermissionGate permissionGate
    ) {
        this(client, registry, config, initialOutputTokenLimit, environmentContextProvider,
                environmentReminderFormatter, permissionGate, null);
    }

    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config,
            int initialOutputTokenLimit,
            EnvironmentContextProvider environmentContextProvider,
            EnvironmentReminderFormatter environmentReminderFormatter,
            PermissionGate permissionGate,
            ContextManager contextManager
    ) {
        this(client, registry, config, initialOutputTokenLimit, environmentContextProvider,
                environmentReminderFormatter, permissionGate, contextManager, null);
    }

    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config,
            int initialOutputTokenLimit,
            EnvironmentContextProvider environmentContextProvider,
            EnvironmentReminderFormatter environmentReminderFormatter,
            PermissionGate permissionGate,
            ContextManager contextManager,
            SkillActivator skillActivator
    ) {
        this(client, registry, config, initialOutputTokenLimit, environmentContextProvider,
                environmentReminderFormatter, permissionGate, contextManager, skillActivator, true);
    }

    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config,
            int initialOutputTokenLimit,
            EnvironmentContextProvider environmentContextProvider,
            EnvironmentReminderFormatter environmentReminderFormatter,
            PermissionGate permissionGate,
            ContextManager contextManager,
            SkillActivator skillActivator,
            boolean closeSharedResources
    ) {
        this(client, registry, config, initialOutputTokenLimit, environmentContextProvider,
                environmentReminderFormatter, permissionGate, contextManager, skillActivator,
                closeSharedResources, HookRuntime.NOOP,
                new HookContextFactory(Path.of("")));
    }

    public Agent(
            LlmClient client,
            ToolRegistry registry,
            AgentConfig config,
            int initialOutputTokenLimit,
            EnvironmentContextProvider environmentContextProvider,
            EnvironmentReminderFormatter environmentReminderFormatter,
            PermissionGate permissionGate,
            ContextManager contextManager,
            SkillActivator skillActivator,
            boolean closeSharedResources,
            HookRuntime hooks,
            HookContextFactory hookContexts
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
        this.permissionGate = permissionGate;
        this.contextManager = contextManager;
        this.skillActivator = skillActivator;
        this.closeSharedResources = closeSharedResources;
        this.hooks = Objects.requireNonNullElse(hooks, HookRuntime.NOOP);
        this.hookContexts = Objects.requireNonNull(hookContexts, "hookContexts 不能为空");
        if (initialOutputTokenLimit <= 0) {
            throw new IllegalArgumentException("initialOutputTokenLimit 必须为正数");
        }
        this.initialOutputTokenLimit = initialOutputTokenLimit;
        this.turnExecutor = new StreamingTurnExecutor(
                client, registry, config.maxParallelTools(), permissionGate, this.hooks, this.hookContexts);
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
        ToolSelection baseSelection = PlanModePrompt.toolSelection(taskMode);
        List<SystemReminder> sessionReminders = List.copyOf(request.reminders());
        ManagedConversationState conversation = new ManagedConversationState(
                request.committedHistory(), request.userMessage());
        List<ChatMessage> skillSourceHistory = new ArrayList<>(request.committedHistory());
        request.reminders().forEach(reminder -> skillSourceHistory.add(new ChatMessage(
                MessageRole.USER, reminder.wrappedContent())));
        skillSourceHistory.add(request.userMessage());
        SkillRunScope skillScope;
        try {
            skillScope = skillActivator == null
                    ? SkillRunScope.NOOP
                    : skillActivator.beginRun(request.skillInvocation(), skillSourceHistory, checkedListener);
        } catch (RuntimeException exception) {
            activeTask.compareAndSet(context, null);
            throw exception;
        }
        AutoCompactTrackingState compactTracking = new AutoCompactTrackingState();
        int iterations = 0;
        UnknownToolCircuitBreaker unknownTools = new UnknownToolCircuitBreaker();
        ScheduledFuture<?> timeoutFuture = watchdog.schedule(
                () -> context.requestStop(AgentStopReason.TIMEOUT, client),
                config.taskTimeout().toNanos(),
                TimeUnit.NANOSECONDS
        );

        try {
            hooks.runHooks(hookContexts.builder(HookEvent.TURN_START)
                    .message(request.userMessage().toString()).build());
            checkedListener.onEvent(new AgentEvent.TaskStarted(taskMode));

            for (int iteration = 1; iteration <= config.maxIterations(); iteration++) {
                iterations = iteration;
                Optional<AgentStopReason> beforeIteration = context.stopReason();
                if (beforeIteration.isPresent()) {
                    return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iterations - 1,
                            beforeIteration.get(), checkedListener);
                }
                if (context.deadlineReached()) {
                    context.requestStop(AgentStopReason.TIMEOUT, client);
                    return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iterations - 1,
                            AgentStopReason.TIMEOUT, checkedListener);
                }

                checkedListener.onEvent(new AgentEvent.IterationStarted(iteration));
                ToolSelection selection = skillActivator == null
                        ? baseSelection : skillActivator.selectTools(baseSelection);
                SystemReminder environmentReminder = environmentReminderFormatter.format(
                        environmentContextProvider.capture());
                List<SystemReminder> reminders = remindersForIteration(
                        environmentReminder,
                        sessionReminders,
                        taskMode,
                        taskModeSnapshot.includeExitReminder(),
                        iteration);
                if (skillActivator != null) {
                    skillActivator.activeReminder().ifPresent(reminder -> {
                        // 环境提醒必须出现在会话提醒之前，保证每轮稳定注入。
                        reminders.add(1, reminder);
                    });
                }
                if (contextManager != null) {
                    ContextResult managed = contextManager.manage(new ContextRequest(
                                    conversation.committed(), conversation.trajectory(), reminders, selection,
                                    OptionalInt.of(initialOutputTokenLimit), ContextManageMode.AUTO, compactTracking),
                            event -> checkedListener.onEvent(new AgentEvent.ContextChanged(event)));
                    conversation.apply(managed);
                    emitCompactHook(managed, iteration, "auto");
                }
                StreamingTurnResult turn;
                boolean contextRecovered = false;
                while (true) {
                    try {
                        turn = turnExecutor.execute(
                                new ChatRequest(conversation.workingMessages(), reminders, selection,
                                        OptionalInt.of(initialOutputTokenLimit)),
                                iteration, iteration < config.maxIterations(), context,
                                unknownTools, checkedListener);
                        break;
                    } catch (LlmException exception) {
                        if (exception.type() != LlmErrorType.CONTEXT_LIMIT || contextRecovered
                                || contextManager == null || context.toolsExecuted()) {
                            throw exception;
                        }
                        ContextResult recovered = contextManager.manage(new ContextRequest(
                                        conversation.committed(), conversation.trajectory(), reminders, selection,
                                        OptionalInt.of(initialOutputTokenLimit), ContextManageMode.RECOVERY,
                                        compactTracking),
                                event -> checkedListener.onEvent(new AgentEvent.ContextChanged(event)));
                        if (!recovered.compacted()) throw exception;
                        conversation.apply(recovered);
                        emitCompactHook(recovered, iteration, "recovery");
                        contextRecovered = true;
                    }
                }
                ChatResponse response = turn.response();
                conversation.append(response.message());

                Optional<AgentStopReason> afterModel = context.stopReason();
                if (afterModel.isPresent()) {
                    return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iteration,
                            afterModel.get(), checkedListener);
                }

                if (!response.hasToolCalls()) {
                    if (context.tryFinish(AgentStopReason.FINAL_RESPONSE)) {
                        emitSafely(
                                checkedListener,
                                new AgentEvent.TaskCompleted(iteration)
                        );
                        return AgentResult.completed(
                                conversation.committed(),
                                conversation.trajectory(),
                                response,
                                context.toolsExecuted(),
                                context.sideEffectsPossible()
                        );
                    }
                    return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iteration,
                            context.stopReason().orElse(AgentStopReason.CANCELLED),
                            checkedListener);
                }

                if (iteration == config.maxIterations()) {
                    context.tryFinish(AgentStopReason.MAX_ITERATIONS);
                    return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iteration,
                            AgentStopReason.MAX_ITERATIONS, checkedListener);
                }

                List<ToolExecution> executions = turn.toolExecutions();
                if (turn.toolsStarted()) {
                    context.markToolsExecuted();
                }

                Optional<AgentStopReason> afterTools = context.stopReason();
                if (afterTools.isPresent()) {
                    return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iteration,
                            afterTools.get(), checkedListener);
                }
                conversation.append(toToolMessage(executions));
            }

            context.tryFinish(AgentStopReason.MAX_ITERATIONS);
            return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iterations,
                    AgentStopReason.MAX_ITERATIONS, checkedListener);
        } catch (UnknownToolCircuitOpenException exception) {
            if (!context.tryFinish(AgentStopReason.TOO_MANY_UNKNOWN_TOOLS)) {
                AgentStopReason existing = context.stopReason()
                        .orElse(AgentStopReason.TOO_MANY_UNKNOWN_TOOLS);
                return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iterations, existing, checkedListener);
            }
            return stopped(
                    context,
                    conversation.rollbackCommitted(),
                    conversation.trajectory(),
                    iterations,
                    AgentStopReason.TOO_MANY_UNKNOWN_TOOLS,
                    checkedListener);
        } catch (LlmException exception) {
            Optional<AgentStopReason> existing = context.stopReason();
            if (existing.isPresent() && existing.get() != AgentStopReason.ERROR) {
                return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iterations, existing.get(), checkedListener);
            }
            AgentError error = new AgentError(
                    exception.safeMessage(),
                    exception.recoverable(),
                    exception.retryAfter()
            );
            context.tryFinish(AgentStopReason.ERROR);
            emitErrorHook(exception.safeMessage(), iterations);
            return failed(context, conversation.rollbackCommitted(), conversation.trajectory(), iterations, error, checkedListener);
        } catch (RuntimeException exception) {
            Optional<AgentStopReason> existing = context.stopReason();
            if (existing.isPresent() && existing.get() != AgentStopReason.ERROR) {
                return stopped(context, conversation.rollbackCommitted(), conversation.trajectory(), iterations, existing.get(), checkedListener);
            }
            AgentError error = new AgentError("Agent 执行失败", false);
            context.tryFinish(AgentStopReason.ERROR);
            emitErrorHook(exception.getMessage(), iterations);
            return failed(context, conversation.rollbackCommitted(), conversation.trajectory(), iterations, error, checkedListener);
        } finally {
            emitTurnEndHook(context, iterations);
            timeoutFuture.cancel(false);
            activeTask.compareAndSet(context, null);
            skillScope.close();
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
        return new ArrayList<>(combined);
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
            List<ChatMessage> committedHistory,
            List<ChatMessage> trajectory,
            int iterations,
            AgentStopReason reason,
            AgentEventListener listener
    ) {
        emitSafely(listener, new AgentEvent.TaskStopped(
                reason, Math.max(iterations, 0), context.sideEffectsPossible()));
        return AgentResult.stopped(
                reason,
                committedHistory,
                trajectory,
                context.toolsExecuted(),
                context.sideEffectsPossible()
        );
    }

    private static AgentResult failed(
            AgentTaskContext context,
            List<ChatMessage> committedHistory,
            List<ChatMessage> trajectory,
            int iterations,
            AgentError error,
            AgentEventListener listener
    ) {
        emitSafely(listener, new AgentEvent.TaskFailed(
                Math.max(iterations, 0), error, context.sideEffectsPossible()));
        return AgentResult.failed(
                committedHistory,
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
        if (permissionGate != null) {
            permissionGate.cancelPending();
        }
    }

    /** 会话空闲时强制压缩已提交历史。 */
    public ContextResult forceCompactHistory(List<ChatMessage> history, AgentEventListener listener) {
        Objects.requireNonNull(history, "history");
        ensureOpen();
        if (activeTask.get() != null) throw new IllegalStateException("Agent 正在执行任务，暂不能压缩");
        if (contextManager == null || history.isEmpty()) {
            return new ContextResult(history, List.of(), history, 0, 0, 0, false,
                    io.imiocode.context.ContextOutcome.UNCHANGED);
        }
        AgentEventListener events = Objects.requireNonNullElse(listener, AgentEventListener.NOOP);
        ContextResult result = contextManager.manage(new ContextRequest(
                        history, List.of(), List.of(), PlanModePrompt.toolSelection(mode()),
                        OptionalInt.of(initialOutputTokenLimit), ContextManageMode.FORCE,
                        new AutoCompactTrackingState()),
                event -> events.onEvent(new AgentEvent.ContextChanged(event)));
        emitCompactHook(result, 0, "manual");
        return result;
    }

    private void emitCompactHook(ContextResult result, int iteration, String mode) {
        if (!result.compacted()) return;
        try {
            HookContext.Builder builder = hookContexts.builder(HookEvent.COMPACT)
                    .data("mode", mode)
                    .data("before_tokens", result.beforeTokens())
                    .data("after_tokens", result.afterTokens());
            if (iteration > 0) builder.iteration(iteration);
            hooks.runHooks(builder.build());
        } catch (RuntimeException ignored) {
            // 压缩已经成功，Hook 失败不能回滚上下文。
        }
    }

    private void emitErrorHook(String message, int iteration) {
        try {
            HookContext.Builder builder = hookContexts.builder(HookEvent.ERROR)
                    .error(message == null || message.isBlank() ? "Agent 执行失败" : message);
            if (iteration > 0) builder.iteration(iteration);
            hooks.runHooks(builder.build());
        } catch (RuntimeException ignored) {
            // error Hook 失败不得递归触发 error，也不得覆盖原错误。
        }
    }

    private void emitTurnEndHook(AgentTaskContext context, int iterations) {
        try {
            HookContext.Builder builder = hookContexts.builder(HookEvent.TURN_END)
                    .data("status", context.stopReason().map(value -> value.name().toLowerCase(java.util.Locale.ROOT))
                            .orElse("failed"));
            if (iterations > 0) builder.iteration(iterations);
            hooks.runHooks(builder.build());
        } catch (RuntimeException ignored) {
            // 终态已确定，Hook 失败只能由通知呈现。
        }
    }

    public boolean respondPermission(String requestId, PermissionReply reply) {
        return permissionGate != null && permissionGate.resolve(requestId, reply);
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
            if (closeSharedResources) {
                if (permissionGate != null) permissionGate.close();
                client.close();
            }
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
