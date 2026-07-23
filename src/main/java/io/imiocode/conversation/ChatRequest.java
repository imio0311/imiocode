package io.imiocode.conversation;

import java.util.List;

public record ChatRequest(List<ChatMessage> messages) {
    public ChatRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("消息列表不能为空");
        }
        messages = List.copyOf(messages);
    }
}
