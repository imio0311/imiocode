package io.imiocode.session;

import java.time.Instant;

public record SessionSummary(SessionId id, Instant createdAt, Instant updatedAt, int messageCount) {
}
