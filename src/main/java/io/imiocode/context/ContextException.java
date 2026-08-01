package io.imiocode.context;

public final class ContextException extends Exception {
    private final boolean recoverable;

    public ContextException(String safeMessage, boolean recoverable) {
        super(safeMessage);
        this.recoverable = recoverable;
    }

    public ContextException(String safeMessage, boolean recoverable, Throwable cause) {
        super(safeMessage, cause);
        this.recoverable = recoverable;
    }

    public boolean recoverable() { return recoverable; }
}
