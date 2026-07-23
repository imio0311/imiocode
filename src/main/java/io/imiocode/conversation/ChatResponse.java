package io.imiocode.conversation;

import io.imiocode.tool.ToolCall;

import java.util.List;
import java.util.Objects;

public record ChatResponse(ChatMessage message) {
    public ChatResponse {
        Objects.requireNonNull(message, "message");
        if (message.role() != MessageRole.ASSISTANT) {
            throw new IllegalArgumentException("模型回复必须是助手消息");
        }
    }

    public ChatResponse(String content) {
        this(new ChatMessage(MessageRole.ASSISTANT, content));
    }

    public String content() {
        return text();
    }

    public String text() {
        return message.content();
    }

    public List<ToolCall> toolCalls() {
        return message.parts().stream()
                .filter(ToolCallPart.class::isInstance)
                .map(ToolCallPart.class::cast)
                .map(ToolCallPart::call)
                .toList();
    }

    public boolean hasToolCalls() {
        return !toolCalls().isEmpty();
    }
}
