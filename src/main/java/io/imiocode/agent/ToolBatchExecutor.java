package io.imiocode.agent;

import io.imiocode.tool.ToolExecution;
import io.imiocode.tool.ToolExecutor;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 按分区策略执行工具：连续低风险工具限流并发，其余工具逐个串行。
 */
public final class ToolBatchExecutor implements AutoCloseable {
    private final ToolRegistry registry;
    private final ToolCallPartitioner partitioner;
    private final ToolExecutor toolExecutor;
    private final ExecutorService parallelExecutor;
    private final Runnable unsafeExecutionStarted;
    private final Set<Future<?>> activeFutures = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public ToolBatchExecutor(ToolRegistry registry, int maxParallelTools) {
        this(registry, maxParallelTools, () -> {
        });
    }

    public ToolBatchExecutor(
            ToolRegistry registry,
            int maxParallelTools,
            Runnable unsafeExecutionStarted
    ) {
        this.registry = Objects.requireNonNull(registry, "registry 不能为空");
        if (maxParallelTools <= 0) {
            throw new IllegalArgumentException("maxParallelTools 必须大于 0");
        }
        this.partitioner = new ToolCallPartitioner(registry);
        this.toolExecutor = new ToolExecutor(registry);
        this.parallelExecutor = Executors.newFixedThreadPool(
                maxParallelTools,
                Thread.ofVirtual().name("imio-tool-", 0).factory()
        );
        this.unsafeExecutionStarted =
                Objects.requireNonNull(unsafeExecutionStarted, "unsafeExecutionStarted 不能为空");
    }

    public List<ToolExecution> execute(
            List<io.imiocode.tool.ToolCall> calls,
            ToolSelection selection,
            int iteration,
            AgentEventListener listener
    ) {
        Objects.requireNonNull(calls, "calls 不能为空");
        Objects.requireNonNull(selection, "selection 不能为空");
        if (iteration <= 0) {
            throw new IllegalArgumentException("iteration 必须大于 0");
        }
        AgentEventListener checkedListener =
                Objects.requireNonNullElse(listener, AgentEventListener.NOOP);
        ensureOpen();

        List<IndexedExecution> results = new ArrayList<>();
        List<ToolBatch> batches = partitioner.partition(calls, selection);
        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            if (cancelled.get()) {
                break;
            }
            ToolBatch batch = batches.get(batchIndex);
            checkedListener.onEvent(new AgentEvent.ToolBatchStarted(
                    iteration, batchIndex, batch.kind(), batch.calls().size()));

            if (batch.kind() == ToolBatchKind.PARALLEL_SAFE) {
                results.addAll(executeParallel(batch, selection, iteration, checkedListener));
            } else {
                IndexedToolCall indexed = batch.calls().getFirst();
                markPossibleSideEffect(indexed, selection);
                results.add(executeOne(indexed, selection, iteration, checkedListener));
            }

            if (!cancelled.get()) {
                checkedListener.onEvent(new AgentEvent.ToolBatchCompleted(
                        iteration, batchIndex, batch.kind(), batch.calls().size()));
            }
        }
        return results.stream()
                .sorted(Comparator.comparingInt(IndexedExecution::index))
                .map(IndexedExecution::execution)
                .toList();
    }

    private List<IndexedExecution> executeParallel(
            ToolBatch batch,
            ToolSelection selection,
            int iteration,
            AgentEventListener listener
    ) {
        List<Future<IndexedExecution>> futures = new ArrayList<>();
        for (IndexedToolCall call : batch.calls()) {
            if (cancelled.get()) {
                break;
            }
            Future<IndexedExecution> future = parallelExecutor.submit(
                    () -> executeOne(call, selection, iteration, listener));
            activeFutures.add(future);
            futures.add(future);
        }

        List<IndexedExecution> executions = new ArrayList<>();
        for (int index = 0; index < futures.size(); index++) {
            Future<IndexedExecution> future = futures.get(index);
            IndexedToolCall call = batch.calls().get(index);
            try {
                executions.add(future.get());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                cancel();
                executions.add(interruptedExecution(call));
            } catch (CancellationException exception) {
                executions.add(interruptedExecution(call));
            } catch (ExecutionException exception) {
                if (exception.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                executions.add(failedExecution(call));
            } finally {
                activeFutures.remove(future);
            }
        }
        return executions;
    }

    private IndexedExecution executeOne(
            IndexedToolCall indexed,
            ToolSelection selection,
            int iteration,
            AgentEventListener listener
    ) {
        ToolExecution execution = toolExecutor.execute(
                indexed.call(),
                selection,
                event -> listener.onEvent(new AgentEvent.ToolExecutionChanged(
                        iteration,
                        indexed.originalIndex(),
                        event
                ))
        );
        return new IndexedExecution(indexed.originalIndex(), execution);
    }

    private void markPossibleSideEffect(IndexedToolCall indexed, ToolSelection selection) {
        registry.findEnabled(indexed.call().name(), selection)
                .filter(tool -> tool.definition().risk() != ToolRisk.LOW)
                .ifPresent(tool -> unsafeExecutionStarted.run());
    }

    private static IndexedExecution interruptedExecution(IndexedToolCall indexed) {
        return new IndexedExecution(
                indexed.originalIndex(),
                new ToolExecution(
                        indexed.call(),
                        ToolResult.interrupted("", "工具执行已中断", false)
                )
        );
    }

    private static IndexedExecution failedExecution(IndexedToolCall indexed) {
        return new IndexedExecution(
                indexed.originalIndex(),
                new ToolExecution(
                        indexed.call(),
                        ToolResult.failure("工具并发任务执行失败")
                )
        );
    }

    public void cancel() {
        cancelled.set(true);
        toolExecutor.cancel();
        for (Future<?> future : activeFutures) {
            future.cancel(true);
        }
        parallelExecutor.shutdownNow();
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("ToolBatchExecutor 已关闭");
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            cancel();
            toolExecutor.close();
            try {
                parallelExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private record IndexedExecution(int index, ToolExecution execution) {
    }
}
