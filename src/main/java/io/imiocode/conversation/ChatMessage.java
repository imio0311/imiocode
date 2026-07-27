package io.imiocode.conversation;

import java.util.List;
import java.util.Objects;

public record ChatMessage(MessageRole role, List<MessagePart> parts) {
    public ChatMessage {
        Objects.requireNonNull(role, "role");
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        parts = List.copyOf(parts);
        validateParts(role, parts);
    }

    public ChatMessage(MessageRole role, String content) {
        this(role, List.of(new TextPart(requireText(content))));
    }

    public String content() {
        return parts.stream()
                .filter(TextPart.class::isInstance)
                .map(TextPart.class::cast)
                .map(TextPart::text)
                .reduce("", String::concat);
    }

    private static void validateParts(MessageRole role, List<MessagePart> parts) {
        for (MessagePart part : parts) {
            Objects.requireNonNull(part, "消息部分");
            boolean valid = switch (role) {
                case USER -> part instanceof TextPart;
                case ASSISTANT -> part instanceof TextPart
                        || part instanceof ThinkingPart
                        || part instanceof ToolCallPart;
                case TOOL -> part instanceof ToolResultPart;
            };
            if (!valid) {
                throw new IllegalArgumentException("消息角色与内容类型不匹配");
            }
        }
    }

    private static String requireText(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        return content;
    }
}
