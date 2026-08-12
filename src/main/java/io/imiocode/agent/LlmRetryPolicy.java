package io.imiocode.agent;

import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 根据错误类型、副作用边界和剩余时间决定是否重试一次 LLM 调用。
 *
 * <p>工具一旦开始执行便停止自动重试，避免重复产生外部副作用；输出上限错误则只提升 Token 上限。</p>
 */
public final class LlmRetryPolicy {
    public static final int MAX_RETRIES = 3;
    public static final int OUTPUT_TOKEN_CEILING = 64_000;
    private static final long[] BACKOFF_SECONDS = {1, 2, 4};

    public Optional<RetryDecision> decide(
            LlmException error,
            int completedRetries,
            int currentOutputTokenLimit,
            Duration taskTimeRemaining,
            boolean toolStarted
    ) {
        Objects.requireNonNull(error, "error 不能为空");
        Objects.requireNonNull(taskTimeRemaining, "taskTimeRemaining 不能为空");
        if (completedRetries < 0 || currentOutputTokenLimit <= 0) {
            throw new IllegalArgumentException("重试次数和 token 上限无效");
        }
        if (toolStarted || completedRetries >= MAX_RETRIES
                || taskTimeRemaining.isZero() || taskTimeRemaining.isNegative()) {
            return Optional.empty();
        }

        Duration delay;
        int nextLimit = currentOutputTokenLimit;
        if (error.type() == LlmErrorType.OUTPUT_LIMIT) {
            if (currentOutputTokenLimit >= OUTPUT_TOKEN_CEILING) {
                return Optional.empty();
            }
            nextLimit = (int) Math.min(
                    (long) currentOutputTokenLimit * 2L,
                    OUTPUT_TOKEN_CEILING);
            delay = Duration.ZERO;
        } else if (error.type() == LlmErrorType.RATE_LIMIT) {
            delay = error.retryAfter().orElse(Duration.ofSeconds(BACKOFF_SECONDS[completedRetries]));
        } else if (error.type() == LlmErrorType.NETWORK
                || error.type() == LlmErrorType.SERVER_ERROR
                || error.type() == LlmErrorType.TIMEOUT) {
            delay = Duration.ofSeconds(BACKOFF_SECONDS[completedRetries]);
        } else {
            return Optional.empty();
        }
        if (delay.compareTo(taskTimeRemaining) >= 0) {
            return Optional.empty();
        }
        return Optional.of(new RetryDecision(
                completedRetries + 2,
                error.type(),
                delay,
                nextLimit
        ));
    }
}
