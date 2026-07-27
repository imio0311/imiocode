package io.imiocode.conversation;

import java.util.List;

public record ChatRequest(List<ChatMessage> messages, List<SystemReminder> reminders) {
    public ChatRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("消息列表不能为空");
        }
        messages = List.copyOf(messages);
        reminders = reminders == null ? List.of() : List.copyOf(reminders);
    }

    public ChatRequest(List<ChatMessage> messages) {
        this(messages, List.of());
    }
}
