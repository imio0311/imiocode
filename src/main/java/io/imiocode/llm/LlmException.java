package io.imiocode.llm;

import java.time.Duration;
import java.util.Optional;

public final class LlmException extends Exception {
    private final LlmErrorType type;
    private final boolean recoverable;
    private final Integer statusCode;
    private final String safeMessage;
    private final Duration retryAfter;

    public LlmException(LlmErrorType type, boolean recoverable, Integer statusCode, String safeMessage) {
        this(type, recoverable, statusCode, safeMessage, null, null);
    }

    public LlmException(
            LlmErrorType type,
            boolean recoverable,
            Integer statusCode,
            String safeMessage,
            Throwable cause) {
        this(type, recoverable, statusCode, safeMessage, null, cause);
    }

    public LlmException(
            LlmErrorType type,
            boolean recoverable,
            Integer statusCode,
            String safeMessage,
            Duration retryAfter,
            Throwable cause) {
        super(safeMessage, cause);
        this.type = type;
        this.recoverable = recoverable;
        this.statusCode = statusCode;
        this.safeMessage = safeMessage;
        if (retryAfter != null && retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter 不能为负数");
        }
        this.retryAfter = retryAfter;
    }

    public LlmErrorType type() {
        return type;
    }

    public boolean recoverable() {
        return recoverable;
    }

    public Integer statusCode() {
        return statusCode;
    }

    public String safeMessage() {
        return safeMessage;
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
