package io.imiocode.agent;

import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.permission.PermissionGate;
import io.imiocode.tool.ToolExecution;
import io.imiocode.tool.ToolRegistry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 执行一个 Agent 轮次，并在工具尚未启动时处理模型级重试。
 */
public final class StreamingTurnExecutor {
    private final LlmClient client;
    private final ToolRegistry registry;
    private final StreamingResponseCollector collector;
    private final LlmRetryPolicy retryPolicy;
    private final RetryWaiter retryWaiter;
    private final int maxParallelTools;
    private final PermissionGate permissionGate;

    public StreamingTurnExecutor(
            LlmClient client,
            ToolRegistry registry,
            int maxParallelTools
    ) {
        this(client, registry, maxParallelTools, new LlmRetryPolicy(), new DefaultRetryWaiter(), null);
    }

    public StreamingTurnExecutor(
            LlmClient client,
            ToolRegistry registry,
            int maxParallelTools,
            PermissionGate permissionGate
    ) {
        this(client, registry, maxParallelTools, new LlmRetryPolicy(), new DefaultRetryWaiter(),
                permissionGate);
    }

    StreamingTurnExecutor(
            LlmClient client,
            ToolRegistry registry,
            int maxParallelTools,
            LlmRetryPolicy retryPolicy,
            RetryWaiter retryWaiter
    ) {
        this(client, registry, maxParallelTools, retryPolicy, retryWaiter, null);
    }

    StreamingTurnExecutor(
            LlmClient client,
            ToolRegistry registry,
            int maxParallelTools,
            LlmRetryPolicy retryPolicy,
            RetryWaiter retryWaiter,
            PermissionGate permissionGate
    ) {
        this.client = Objects.requireNonNull(client, "client 不能为空");
        this.registry = Objects.requireNonNull(registry, "registry 不能为空");
        if (maxParallelTools <= 0) {
            throw new IllegalArgumentException("maxParallelTools 必须为正数");
        }
        this.maxParallelTools = maxParallelTools;
        this.collector = new StreamingResponseCollector(client);
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy 不能为空");
        this.retryWaiter = Objects.requireNonNull(retryWaiter, "retryWaiter 不能为空");
        this.permissionGate = permissionGate;
    }

    public StreamingTurnResult execute(
            ChatRequest request,
            int iteration,
            boolean toolsMayExecute,
            AgentTaskContext task,
            UnknownToolCircuitBreaker breaker,
            AgentEventListener listener
    ) throws LlmException {
        Objects.requireNonNull(request, "request 不能为空");
        Objects.requireNonNull(task, "task 不能为空");
        Objects.requireNonNull(breaker, "breaker 不能为空");
        AgentEventListener events = Objects.requireNonNullElse(listener, AgentEventListener.NOOP);
        int retries = 0;
        int currentLimit = request.outputTokenLimit().orElseThrow(
                () -> new IllegalArgumentException("Agent 请求必须携带输出 token 上限"));

        while (true) {
            int attempt = retries + 1;
            UnknownToolCircuitBreaker.Attempt circuitAttempt = breaker.beginAttempt();
            AtomicCircuitState circuitState = new AtomicCircuitState();
            try (StreamingToolScheduler scheduler = new StreamingToolScheduler(
                    registry,
                    request.toolSelection(),
                    iteration,
                    maxParallelTools,
                    toolsMayExecute,
                    circuitAttempt,
                    events,
                    () -> {
                        circuitState.open = true;
                        client.cancelActiveRequest();
                    },
                    task::markSideEffectsPossible,
                    permissionGate
            )) {
                Runnable cancellation = scheduler::cancel;
                task.attachCancellation(cancellation);
                try {
                    ChatRequest attemptRequest = new ChatRequest(
                            request.messages(),
                            request.reminders(),
                            request.toolSelection(),
                            OptionalInt.of(currentLimit));
                    ChatResponse response = collector.collect(
                            attemptRequest,
                            iteration,
                            attempt,
                            events,
                            scheduler::onToolCallCompleted);
                    scheduler.ensureResponseCalls(response.toolCalls());
                    if (circuitState.open) {
                        throw new UnknownToolCircuitOpenException();
                    }
                    scheduler.onStreamCompleted();
                    List<ToolExecution> executions = scheduler.awaitResults();
                    circuitAttempt.commit();
                    return new StreamingTurnResult(
                            response, executions, scheduler.toolsStarted());
                } catch (UnknownToolCircuitOpenException exception) {
                    if (scheduler.toolsStarted()) {
                        task.markToolsExecuted();
                    }
                    scheduler.cancel();
                    throw exception;
                } catch (LlmException exception) {
                    boolean toolStarted = scheduler.toolsStarted();
                    if (toolStarted) {
                        task.markToolsExecuted();
                    }
                    scheduler.cancel();
                    Optional<RetryDecision> decision = retryPolicy.decide(
                            exception,
                            retries,
                            currentLimit,
                            task.remainingTime(),
                            toolStarted);
                    if (decision.isEmpty()) {
                        throw exception;
                    }
                    RetryDecision retry = decision.orElseThrow();
                    events.onEvent(new AgentEvent.RetryScheduled(
                            iteration,
                            retry.nextAttempt(),
                            retry.reason(),
                            retry.delay(),
                            retry.outputTokenLimit()));
                    try {
                        retryWaiter.await(retry.delay(), task);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new LlmException(
                                LlmErrorType.INTERRUPTED,
                                false,
                                null,
                                "模型重试等待已中断",
                                interrupted);
                    }
                    if (task.stopReason().isPresent()) {
                        throw new LlmException(
                                LlmErrorType.INTERRUPTED,
                                false,
                                null,
                                "Agent 任务已停止");
                    }
                    retries++;
                    currentLimit = retry.outputTokenLimit();
                } finally {
                    task.clearCancellation(cancellation);
                }
            }
        }
    }

    private static final class AtomicCircuitState {
        private volatile boolean open;
    }
}
