package io.imiocode.conversation;

import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;

import java.util.Objects;
import java.time.Duration;
import java.util.Optional;

/** 会话编排层对外暴露的安全异常。 */
public final class ConversationException extends Exception {
    private final String safeMessage;
    private final boolean recoverable;
    private final boolean interrupted;
    private final boolean toolsExecuted;
    private final Duration retryAfter;

    public ConversationException(
            String safeMessage,
            boolean recoverable,
            boolean interrupted,
            boolean toolsExecuted) {
        this(safeMessage, recoverable, interrupted, toolsExecuted, null, null);
    }

    public ConversationException(
            String safeMessage,
            boolean recoverable,
            boolean interrupted,
            boolean toolsExecuted,
            Throwable cause) {
        this(safeMessage, recoverable, interrupted, toolsExecuted, null, cause);
    }

    public ConversationException(
            String safeMessage,
            boolean recoverable,
            boolean interrupted,
            boolean toolsExecuted,
            Duration retryAfter,
            Throwable cause) {
        super(requireText(safeMessage), cause);
        this.safeMessage = safeMessage;
        this.recoverable = recoverable;
        this.interrupted = interrupted;
        this.toolsExecuted = toolsExecuted;
        this.retryAfter = retryAfter;
    }

    public static ConversationException from(LlmException exception, boolean toolsExecuted) {
        Objects.requireNonNull(exception, "exception");
        return new ConversationException(
                exception.safeMessage(),
                exception.recoverable(),
                exception.type() == LlmErrorType.INTERRUPTED,
                toolsExecuted,
                exception.retryAfter().orElse(null),
                exception);
    }

    public String safeMessage() {
        return safeMessage;
    }

    public boolean recoverable() {
        return recoverable;
    }

    public boolean interrupted() {
        return interrupted;
    }

    public boolean toolsExecuted() {
        return toolsExecuted;
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("safeMessage 不能为空");
        }
        return value;
    }
}
