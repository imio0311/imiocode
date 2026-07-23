package io.imiocode.llm;

public final class LlmException extends Exception {
    private final LlmErrorType type;
    private final boolean recoverable;
    private final Integer statusCode;
    private final String safeMessage;

    public LlmException(LlmErrorType type, boolean recoverable, Integer statusCode, String safeMessage) {
        this(type, recoverable, statusCode, safeMessage, null);
    }

    public LlmException(
            LlmErrorType type,
            boolean recoverable,
            Integer statusCode,
            String safeMessage,
            Throwable cause) {
        super(safeMessage, cause);
        this.type = type;
        this.recoverable = recoverable;
        this.statusCode = statusCode;
        this.safeMessage = safeMessage;
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
}
