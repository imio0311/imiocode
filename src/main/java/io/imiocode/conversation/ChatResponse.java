package io.imiocode.conversation;

import io.imiocode.tool.ToolCall;
import io.imiocode.llm.TokenUsage;

import java.util.List;
import java.util.Objects;

/** 表示已完整组装的助手消息及其可选 Token 用量。 */
public record ChatResponse(ChatMessage message, TokenUsage usage) {
    public ChatResponse {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(usage, "usage");
        if (message.role() != MessageRole.ASSISTANT) {
            throw new IllegalArgumentException("模型回复必须是助手消息");
        }
    }

    public ChatResponse(String content) {
        this(new ChatMessage(MessageRole.ASSISTANT, content), TokenUsage.unknown());
    }

    public ChatResponse(ChatMessage message) {
        this(message, TokenUsage.unknown());
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
