package io.imiocode.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** 按模型给出的顺序同步执行工具。 */
public final class ToolExecutor implements AutoCloseable {
    private final ToolRegistry registry;
    private final ConcurrentHashMap<Long, Tool> activeTools = new ConcurrentHashMap<>();
    private final AtomicLong executionSequence = new AtomicLong();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public ToolExecutor(ToolRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public ToolRegistry registry() {
        return registry;
    }

    public List<ToolExecution> executeAll(
            List<ToolCall> calls,
            ToolExecutionListener listener) {
        Objects.requireNonNull(calls, "calls");
        ToolExecutionListener checkedListener =
                Objects.requireNonNullElse(listener, ToolExecutionListener.NOOP);
        List<ToolExecution> executions = new ArrayList<>();
        for (ToolCall call : List.copyOf(calls)) {
            if (cancelled.get() || closed.get()) {
                break;
            }
            executions.add(execute(call, ToolSelection.allEnabled(), checkedListener));
            if (cancelled.get()) {
                break;
            }
        }
        return List.copyOf(executions);
    }

    /**
     * 执行一个工具调用。该方法可被多个虚拟线程并发调用。
     */
    public ToolExecution execute(
            ToolCall call,
            ToolSelection selection,
            ToolExecutionListener listener) {
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(selection, "selection");
        ToolExecutionListener checkedListener =
                Objects.requireNonNullElse(listener, ToolExecutionListener.NOOP);

        checkedListener.onToolEvent(new ToolExecutionEvent(
                ToolExecutionState.QUEUED, call, null));
        if (cancelled.get() || closed.get()) {
            return failedExecution(
                    call,
                    ToolResult.interrupted("", "工具执行已取消", false),
                    checkedListener);
        }

        Optional<Tool> candidate = registry.findEnabled(call.name(), selection);
        if (candidate.isEmpty()) {
            return failedExecution(
                    call,
                    ToolResult.failure("未知、已禁用或当前模式不允许的工具: " + call.name()),
                    checkedListener);
        }

        Tool tool = candidate.get();
        long executionId = executionSequence.incrementAndGet();
        activeTools.put(executionId, tool);
        try {
            checkedListener.onToolEvent(new ToolExecutionEvent(
                    ToolExecutionState.RUNNING, call, null));
        } catch (RuntimeException exception) {
            activeTools.remove(executionId);
            throw exception;
        }
        ToolResult result;
        try {
            result = tool.execute(call.arguments());
            if (cancelled.get() && result.success()) {
                result = ToolResult.interrupted(
                        result.output(),
                        "工具执行已取消",
                        result.truncated());
            }
        } catch (RuntimeException exception) {
            result = ToolResult.failure("工具执行失败");
        } finally {
            activeTools.remove(executionId);
        }

        checkedListener.onToolEvent(new ToolExecutionEvent(
                result.success() ? ToolExecutionState.SUCCEEDED : ToolExecutionState.FAILED,
                call,
                result));
        return new ToolExecution(call, result);
    }

    private static ToolExecution failedExecution(
            ToolCall call,
            ToolResult result,
            ToolExecutionListener listener) {
        listener.onToolEvent(new ToolExecutionEvent(
                ToolExecutionState.FAILED, call, result));
        return new ToolExecution(call, result);
    }

    public void cancel() {
        cancelled.set(true);
        for (Tool tool : activeTools.values()) {
            try {
                tool.cancel();
            } catch (RuntimeException ignored) {
                // 取消应尽力而为，单个工具失败不能阻止其余工具被取消。
            }
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            cancel();
        }
    }
}
