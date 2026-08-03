package io.imiocode.session;

import io.imiocode.conversation.ChatMessage;
import java.util.List;
import java.util.Objects;

public record SessionSnapshot(SessionMetadata metadata, List<ChatMessage> history) {
    public SessionSnapshot {
        Objects.requireNonNull(metadata, "metadata");
        history = List.copyOf(history == null ? List.of() : history);
        if (metadata.messageCount() != history.size()) throw new IllegalArgumentException("消息数与历史不一致");
    }
}
