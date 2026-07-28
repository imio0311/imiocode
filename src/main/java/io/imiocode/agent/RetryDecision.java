package io.imiocode.agent;

import io.imiocode.llm.LlmErrorType;

import java.time.Duration;
import java.util.Objects;

public record RetryDecision(
        int nextAttempt,
        LlmErrorType reason,
        Duration delay,
        int outputTokenLimit
) {
    public RetryDecision {
        if (nextAttempt < 2 || nextAttempt > 4) {
            throw new IllegalArgumentException("nextAttempt 必须在 2 到 4 之间");
        }
        reason = Objects.requireNonNull(reason, "reason 不能为空");
        delay = Objects.requireNonNull(delay, "delay 不能为空");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay 不能为负数");
        }
        if (outputTokenLimit <= 0) {
            throw new IllegalArgumentException("outputTokenLimit 必须为正数");
        }
    }
}
