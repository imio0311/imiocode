package io.imiocode.tool;

import java.util.Objects;

public record ToolExecutionEvent(
        ToolExecutionState state,
        ToolCall call,
        ToolResult result) {
    public ToolExecutionEvent {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(call, "call");
        if ((state == ToolExecutionState.SUCCEEDED || state == ToolExecutionState.FAILED) && result == null) {
            throw new IllegalArgumentException("完成事件必须包含工具结果");
        }
    }
}
