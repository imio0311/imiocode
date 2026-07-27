package io.imiocode.conversation;

import io.imiocode.tool.ToolSelection;

import java.util.List;

public record ChatRequest(
        List<ChatMessage> messages,
        List<SystemReminder> reminders,
        ToolSelection toolSelection) {
    public ChatRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("消息列表不能为空");
        }
        messages = List.copyOf(messages);
        reminders = reminders == null ? List.of() : List.copyOf(reminders);
        toolSelection = toolSelection == null ? ToolSelection.allEnabled() : toolSelection;
    }

    public ChatRequest(List<ChatMessage> messages, List<SystemReminder> reminders) {
        this(messages, reminders, ToolSelection.allEnabled());
    }

    public ChatRequest(List<ChatMessage> messages) {
        this(messages, List.of(), ToolSelection.allEnabled());
    }
}
