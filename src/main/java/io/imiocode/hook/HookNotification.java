package io.imiocode.hook;

import java.time.Instant;
import java.util.Objects;

public record HookNotification(Instant occurredAt, String hookId, HookEvent event,
                               HookExecutionStatus status, String summary) {
    public HookNotification {
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        hookId = Objects.requireNonNull(hookId, "hookId");
        event = Objects.requireNonNull(event, "event");
        status = Objects.requireNonNull(status, "status");
        summary = Objects.requireNonNullElse(summary, "");
    }
}
