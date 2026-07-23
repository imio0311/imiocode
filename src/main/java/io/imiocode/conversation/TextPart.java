package io.imiocode.conversation;

import java.util.Objects;

public record TextPart(String text) implements MessagePart {
    public TextPart {
        Objects.requireNonNull(text, "text");
        if (text.isEmpty()) {
            throw new IllegalArgumentException("文本内容不能为空");
        }
    }
}
