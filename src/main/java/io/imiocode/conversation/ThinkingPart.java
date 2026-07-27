package io.imiocode.conversation;

import java.util.Objects;

public record ThinkingPart(String text, ThinkingMetadata metadata) implements MessagePart {
    public ThinkingPart {
        text = Objects.requireNonNullElse(text, "");
        Objects.requireNonNull(metadata, "metadata");
    }
}
