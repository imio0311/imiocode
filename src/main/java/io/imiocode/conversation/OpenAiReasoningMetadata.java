package io.imiocode.conversation;

import java.util.Objects;

public record OpenAiReasoningMetadata(String itemId, String encryptedContent)
        implements ThinkingMetadata {
    public OpenAiReasoningMetadata {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("OpenAI reasoning itemId 不能为空");
        }
        itemId = itemId.trim();
        encryptedContent = Objects.requireNonNullElse(encryptedContent, "");
    }
}
