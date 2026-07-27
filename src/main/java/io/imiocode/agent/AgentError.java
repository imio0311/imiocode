package io.imiocode.agent;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 可安全展示给用户的 Agent 错误。
 */
public record AgentError(
        String safeMessage,
        boolean recoverable,
        Optional<Duration> retryAfter
) {
    public AgentError {
        if (safeMessage == null || safeMessage.isBlank()) {
            throw new IllegalArgumentException("safeMessage 不能为空");
        }
        retryAfter = Objects.requireNonNullElse(retryAfter, Optional.empty());
        retryAfter.ifPresent(duration -> {
            if (duration.isNegative()) {
                throw new IllegalArgumentException("retryAfter 不能为负数");
            }
        });
    }

    public AgentError(String safeMessage, boolean recoverable) {
        this(safeMessage, recoverable, Optional.empty());
    }
}
