package io.imiocode.session;

import java.time.Instant;
import java.util.Objects;

public record SessionMetadata(SessionId id, Instant createdAt, Instant updatedAt,
                              String workspaceIdentity, long commitCount, int messageCount) {
    public SessionMetadata {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (workspaceIdentity == null || workspaceIdentity.isBlank()) throw new IllegalArgumentException("workspaceIdentity 不能为空");
        if (commitCount < 0 || messageCount < 0) throw new IllegalArgumentException("会话计数不能为负数");
    }
}
