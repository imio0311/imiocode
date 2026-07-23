package io.imiocode.conversation;

import io.imiocode.tool.ToolCall;

import java.util.Objects;

public record ToolCallPart(ToolCall call) implements MessagePart {
    public ToolCallPart {
        Objects.requireNonNull(call, "call");
    }
}
