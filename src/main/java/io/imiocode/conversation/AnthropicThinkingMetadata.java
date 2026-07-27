package io.imiocode.conversation;

import java.util.Objects;

public record AnthropicThinkingMetadata(String signature, String redactedData)
        implements ThinkingMetadata {
    public AnthropicThinkingMetadata {
        signature = Objects.requireNonNullElse(signature, "");
        redactedData = Objects.requireNonNullElse(redactedData, "");
        if (signature.isBlank() && redactedData.isBlank()) {
            throw new IllegalArgumentException("Anthropic Thinking 元数据不能为空");
        }
    }
}
