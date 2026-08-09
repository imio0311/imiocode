package io.imiocode.subagent.task;

import java.time.Instant;
import java.util.Optional;

public record TaskSnapshot(String id, String agentName, String description, TaskStatus status,
                           Instant createdAt, Optional<Instant> startedAt, Optional<Instant> finishedAt,
                           Optional<String> output, Optional<String> traceId) {}
