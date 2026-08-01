package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;

import java.util.List;

public record OffloadResult(List<ChatMessage> messages, int spilledCount) {
    public OffloadResult {
        messages = messages == null ? List.of() : List.copyOf(messages);
        if (spilledCount < 0) throw new IllegalArgumentException("spilledCount 不能为负数");
    }
}
