package io.imiocode.subagent.task;

import java.time.Instant;

public record TaskNotification(String taskId, TaskStatus status, String summary, Instant createdAt) {}
