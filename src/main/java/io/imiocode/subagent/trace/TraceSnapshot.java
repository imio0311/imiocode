package io.imiocode.subagent.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record TraceSnapshot(String id, Optional<String> parentId, String agentName, String model,
                            boolean background, TraceStatus status,
                            Instant startedAt, Optional<Instant> finishedAt, TraceTokenUsage usage,
                            List<String> children, Optional<String> stopReason, Optional<String> error) {
    public Duration elapsed(Instant now) {
        return Duration.between(startedAt, finishedAt.orElse(now));
    }
}
