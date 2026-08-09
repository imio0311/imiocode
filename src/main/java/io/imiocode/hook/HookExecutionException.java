package io.imiocode.hook;

public final class HookExecutionException extends RuntimeException {
    private final String hookId;
    public HookExecutionException(String hookId, String safeMessage) {
        super(safeMessage, null, false, false);
        this.hookId = hookId;
    }
    public String hookId() { return hookId; }
}
