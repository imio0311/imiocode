package io.imiocode.conversation;

import java.util.Objects;

public record ChatMessage(MessageRole role, String content) {
    public ChatMessage {
        Objects.requireNonNull(role, "role");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
    }
}
