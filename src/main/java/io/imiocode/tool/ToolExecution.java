package io.imiocode.tool;

import java.util.Objects;

public record ToolExecution(ToolCall call, ToolResult result) {
    public ToolExecution {
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(result, "result");
    }
}
