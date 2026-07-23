package io.imiocode.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 按模型给出的顺序同步执行工具。 */
public final class ToolExecutor implements AutoCloseable {
    private final ToolRegistry registry;
    private final AtomicReference<Tool> activeTool = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public ToolExecutor(ToolRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
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
            checkedListener.onToolEvent(new ToolExecutionEvent(
                    ToolExecutionState.QUEUED, call, null));
            Optional<Tool> candidate = registry.findEnabled(call.name());
            if (candidate.isEmpty()) {
                ToolResult result = ToolResult.failure("未知或已禁用工具: " + call.name());
                executions.add(new ToolExecution(call, result));
                checkedListener.onToolEvent(new ToolExecutionEvent(
                        ToolExecutionState.FAILED, call, result));
                continue;
            }

            Tool tool = candidate.get();
            activeTool.set(tool);
            checkedListener.onToolEvent(new ToolExecutionEvent(
                    ToolExecutionState.RUNNING, call, null));
            ToolResult result;
            try {
                result = tool.execute(call.arguments());
            } catch (RuntimeException exception) {
                result = ToolResult.failure("工具执行失败");
            } finally {
                activeTool.compareAndSet(tool, null);
            }
            executions.add(new ToolExecution(call, result));
            checkedListener.onToolEvent(new ToolExecutionEvent(
                    result.success() ? ToolExecutionState.SUCCEEDED : ToolExecutionState.FAILED,
                    call,
                    result));
            if (cancelled.get()) {
                break;
            }
        }
        return List.copyOf(executions);
    }

    public void cancel() {
        cancelled.set(true);
        Tool tool = activeTool.get();
        if (tool != null) {
            tool.cancel();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            cancel();
        }
    }
}
