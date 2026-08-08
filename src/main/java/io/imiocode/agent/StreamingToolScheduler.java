package io.imiocode.agent;

import io.imiocode.permission.PermissionAction;
import io.imiocode.permission.PermissionDecision;
import io.imiocode.permission.PermissionEvaluation;
import io.imiocode.permission.PermissionGate;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolAvailability;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolExecution;
import io.imiocode.tool.ToolExecutor;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResolution;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 在模型流期间提前执行连续 LOW 前缀，流成功后再跨越串行屏障。
 */
public final class StreamingToolScheduler implements AutoCloseable {
    private final ToolRegistry registry;
    private final ToolSelection selection;
    private final int iteration;
    private final AgentEventListener listener;
    private final boolean toolsMayExecute;
    private final UnknownToolCircuitBreaker.Attempt circuit;
    private final Runnable circuitOpened;
    private final Runnable unsafeExecutionStarted;
    private final PermissionGate permissionGate;
    private final ToolExecutor toolExecutor;
    private final ExecutorService parallelExecutor;
    private final Map<Integer, ToolCall> calls = new TreeMap<>();
    private final Map<Integer, ToolExecution> results = new HashMap<>();
    private final Map<Integer, Future<ToolExecution>> futures = new HashMap<>();
    private final Map<Integer, PermissionEvaluation> permissionEvaluations = new HashMap<>();
    private final AtomicBoolean toolsStarted = new AtomicBoolean();
    private boolean streamCompleted;
    private boolean eagerBarrier;
    private boolean cancelled;
    private int nextObserved;
    private int nextEager;

    public StreamingToolScheduler(
            ToolRegistry registry,
            ToolSelection selection,
            int iteration,
            int maxParallelTools,
            boolean toolsMayExecute,
            UnknownToolCircuitBreaker.Attempt circuit,
            AgentEventListener listener,
            Runnable circuitOpened,
            Runnable unsafeExecutionStarted
    ) {
        this(
                registry,
                selection,
                iteration,
                maxParallelTools,
                toolsMayExecute,
                circuit,
                listener,
                circuitOpened,
                unsafeExecutionStarted,
                null);
    }

    public StreamingToolScheduler(
            ToolRegistry registry,
            ToolSelection selection,
            int iteration,
            int maxParallelTools,
            boolean toolsMayExecute,
            UnknownToolCircuitBreaker.Attempt circuit,
            AgentEventListener listener,
            Runnable circuitOpened,
            Runnable unsafeExecutionStarted,
            PermissionGate permissionGate
    ) {
        this.registry = Objects.requireNonNull(registry, "registry 不能为空");
        this.selection = Objects.requireNonNull(selection, "selection 不能为空");
        if (iteration <= 0 || maxParallelTools <= 0) {
            throw new IllegalArgumentException("iteration 和 maxParallelTools 必须为正数");
        }
        this.iteration = iteration;
        this.listener = Objects.requireNonNullElse(listener, AgentEventListener.NOOP);
        this.toolsMayExecute = toolsMayExecute;
        this.circuit = Objects.requireNonNull(circuit, "circuit 不能为空");
        this.circuitOpened = Objects.requireNonNull(circuitOpened, "circuitOpened 不能为空");
        this.unsafeExecutionStarted =
                Objects.requireNonNull(unsafeExecutionStarted, "unsafeExecutionStarted 不能为空");
        this.permissionGate = permissionGate;
        this.toolExecutor = new ToolExecutor(registry);
        this.parallelExecutor = Executors.newFixedThreadPool(
                maxParallelTools,
                Thread.ofVirtual().name("imio-stream-tool-", 0).factory());
    }

    public synchronized void onToolCallCompleted(int originalIndex, ToolCall call) {
        if (originalIndex < 0) {
            throw new IllegalArgumentException("originalIndex 不能为负数");
        }
        Objects.requireNonNull(call, "call 不能为空");
        ensureActive();
        if (calls.putIfAbsent(originalIndex, call) != null) {
            throw new IllegalStateException("重复的工具调用索引: " + originalIndex);
        }
        observeContinuousCalls();
        startEagerPrefix();
    }

    private void observeContinuousCalls() {
        while (calls.containsKey(nextObserved)) {
            ToolResolution resolution = registry.resolve(
                    calls.get(nextObserved).name(), selection);
            CircuitObservation observation = circuit.observe(resolution);
            nextObserved++;
            if (observation == CircuitObservation.OPENED) {
                cancelled = true;
                cancelRunning();
                circuitOpened.run();
                throw new UnknownToolCircuitOpenException();
            }
        }
    }

    private void startEagerPrefix() {
        if (!toolsMayExecute || eagerBarrier || cancelled) {
            return;
        }
        while (calls.containsKey(nextEager)) {
            ToolCall call = calls.get(nextEager);
            ToolResolution resolution = registry.resolve(call.name(), selection);
            if (resolution.availability() != ToolAvailability.AVAILABLE) {
                results.put(nextEager, unavailableExecution(call, resolution.availability()));
                nextEager++;
                continue;
            }
            Tool tool = resolution.tool().orElseThrow();
            if ("load_skill".equals(call.name())) {
                // fork Skill 可能发起新的模型请求，必须等当前模型流完整结束。
                eagerBarrier = true;
                return;
            }
            PermissionEvaluation evaluation = permissionEvaluation(nextEager, call, tool);
            if (evaluation != null
                    && evaluation.decision().action() == PermissionAction.DENY) {
                results.put(nextEager, permissionDeniedExecution(call, evaluation.decision()));
                nextEager++;
                continue;
            }
            if (evaluation != null
                    && evaluation.decision().action() == PermissionAction.ASK) {
                eagerBarrier = true;
                return;
            }
            if (tool.definition().risk() != ToolRisk.LOW) {
                eagerBarrier = true;
                return;
            }
            startParallel(nextEager, call);
            nextEager++;
        }
    }

    private void startParallel(int index, ToolCall call) {
        toolsStarted.set(true);
        Future<ToolExecution> future = parallelExecutor.submit(() -> executeOne(index, call));
        futures.put(index, future);
    }

    private ToolExecution executeOne(int index, ToolCall call) {
        return toolExecutor.execute(call, selection, event -> listener.onEvent(
                new AgentEvent.ToolExecutionChanged(iteration, index, event)));
    }

    public synchronized void onStreamCompleted() {
        ensureActive();
        streamCompleted = true;
        observeContinuousCalls();
    }

    /**
     * 兼容只实现旧 StreamListener 的客户端：它们返回完整响应但不发布工具完成事件。
     */
    public synchronized void ensureResponseCalls(List<ToolCall> responseCalls) {
        Objects.requireNonNull(responseCalls, "responseCalls 不能为空");
        if (!calls.isEmpty()) {
            return;
        }
        for (int index = 0; index < responseCalls.size(); index++) {
            calls.put(index, responseCalls.get(index));
        }
        observeContinuousCalls();
        startEagerPrefix();
    }

    public List<ToolExecution> awaitResults() {
        synchronized (this) {
            if (!streamCompleted) {
                throw new IllegalStateException("模型流尚未完成");
            }
            if (!toolsMayExecute) {
                return List.of();
            }
        }

        List<Integer> indexes;
        synchronized (this) {
            indexes = new ArrayList<>(calls.keySet());
        }
        int position = 0;
        while (position < indexes.size() && !isCancelled()) {
            int index = indexes.get(position);
            if (hasResultOrFuture(index)) {
                collect(index);
                position++;
                continue;
            }
            ToolCall call = call(index);
            ToolResolution resolution = registry.resolve(call.name(), selection);
            if (resolution.availability() != ToolAvailability.AVAILABLE) {
                putResult(index, unavailableExecution(call, resolution.availability()));
                position++;
                continue;
            }
            Tool tool = resolution.tool().orElseThrow();
            PermissionDecision permission = resolvePermission(index, call, tool);
            if (permission.action() == PermissionAction.DENY) {
                putResult(index, permissionDeniedExecution(call, permission));
                position++;
                continue;
            }
            if (tool.definition().risk() != ToolRisk.LOW) {
                unsafeExecutionStarted.run();
                toolsStarted.set(true);
                putResult(index, executeOne(index, call));
                position++;
                continue;
            }

            List<Integer> safeBatch = new ArrayList<>();
            synchronized (this) {
                startParallel(index, call);
            }
            safeBatch.add(index);
            position++;
            while (position < indexes.size()) {
                int candidateIndex = indexes.get(position);
                if (hasResultOrFuture(candidateIndex)) {
                    break;
                }
                ToolCall candidate = call(candidateIndex);
                ToolResolution candidateResolution =
                        registry.resolve(candidate.name(), selection);
                if (candidateResolution.availability() != ToolAvailability.AVAILABLE
                        || candidateResolution.tool().orElseThrow()
                        .definition().risk() != ToolRisk.LOW) {
                    break;
                }
                PermissionEvaluation candidateEvaluation = permissionEvaluation(
                        candidateIndex,
                        candidate,
                        candidateResolution.tool().orElseThrow());
                if (candidateEvaluation != null
                        && candidateEvaluation.decision().action() != PermissionAction.ALLOW) {
                    break;
                }
                synchronized (this) {
                    startParallel(candidateIndex, candidate);
                }
                safeBatch.add(candidateIndex);
                position++;
            }
            for (int safeIndex : safeBatch) {
                collect(safeIndex);
            }
        }

        synchronized (this) {
            return indexes.stream()
                    .filter(results::containsKey)
                    .map(results::get)
                    .toList();
        }
    }

    private void collect(int index) {
        Future<ToolExecution> future;
        synchronized (this) {
            if (results.containsKey(index)) {
                return;
            }
            future = futures.get(index);
        }
        if (future == null) {
            return;
        }
        ToolExecution execution;
        try {
            execution = future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cancel();
            execution = interruptedExecution(call(index));
        } catch (CancellationException exception) {
            execution = interruptedExecution(call(index));
        } catch (ExecutionException exception) {
            execution = new ToolExecution(call(index), ToolResult.failure("工具并发任务执行失败"));
        }
        putResult(index, execution);
    }

    private synchronized boolean hasResultOrFuture(int index) {
        return results.containsKey(index) || futures.containsKey(index);
    }

    private synchronized ToolCall call(int index) {
        return calls.get(index);
    }

    private synchronized void putResult(int index, ToolExecution execution) {
        results.putIfAbsent(index, execution);
    }

    private synchronized boolean isCancelled() {
        return cancelled;
    }

    public boolean toolsStarted() {
        return toolsStarted.get();
    }

    public synchronized void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        if (permissionGate != null) {
            permissionGate.cancelPending();
        }
        cancelRunning();
    }

    private void cancelRunning() {
        toolExecutor.cancel();
        futures.values().forEach(future -> future.cancel(true));
        parallelExecutor.shutdownNow();
    }

    private void ensureActive() {
        if (cancelled) {
            throw new IllegalStateException("流式工具调度器已取消");
        }
    }

    private static ToolExecution unavailableExecution(
            ToolCall call,
            ToolAvailability availability
    ) {
        String message = switch (availability) {
            case UNKNOWN -> "未知工具: " + call.name();
            case DISABLED -> "工具已禁用: " + call.name();
            case DISALLOWED -> "当前模式不允许工具: " + call.name();
            case AVAILABLE -> throw new IllegalArgumentException("AVAILABLE 不是失败状态");
        };
        return new ToolExecution(call, ToolResult.failure(message));
    }

    private static ToolExecution interruptedExecution(ToolCall call) {
        return new ToolExecution(
                call,
                ToolResult.interrupted("", "工具执行已中断", false));
    }

    private synchronized PermissionEvaluation permissionEvaluation(
            int index,
            ToolCall call,
            Tool tool
    ) {
        if (permissionGate == null) {
            return null;
        }
        return permissionEvaluations.computeIfAbsent(
                index,
                ignored -> permissionGate.evaluate(call, tool));
    }

    private PermissionDecision resolvePermission(int index, ToolCall call, Tool tool) {
        PermissionEvaluation evaluation = permissionEvaluation(index, call, tool);
        if (evaluation == null) {
            return PermissionDecision.allow(
                    io.imiocode.permission.PermissionDecisionSource.MODE,
                    "兼容调用方已允许工具");
        }
        if (evaluation.decision().action() != PermissionAction.ASK) {
            return evaluation.decision();
        }
        return permissionGate.confirm(
                evaluation,
                iteration,
                prompt -> listener.onEvent(new AgentEvent.PermissionRequested(prompt)),
                (requestId, reply) -> listener.onEvent(
                        new AgentEvent.PermissionResolved(requestId, reply)));
    }

    private static ToolExecution permissionDeniedExecution(
            ToolCall call,
            PermissionDecision decision
    ) {
        return new ToolExecution(
                call,
                ToolResult.failure("权限拒绝：" + decision.reason()));
    }

    @Override
    public void close() {
        cancel();
        try {
            parallelExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        toolExecutor.close();
    }
}
