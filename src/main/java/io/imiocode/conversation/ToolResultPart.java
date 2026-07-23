package io.imiocode.conversation;

import io.imiocode.tool.ToolResult;

import java.util.Objects;

public record ToolResultPart(
        String callId,
        String toolName,
        ToolResult result) implements MessagePart {
    public ToolResultPart {
        callId = requireText(callId, "callId");
        toolName = requireText(toolName, "toolName");
        Objects.requireNonNull(result, "result");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
