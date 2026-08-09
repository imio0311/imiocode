package io.imiocode.hook.condition;

public final class ConditionParseException extends IllegalArgumentException {
    public ConditionParseException(String message) { super(message); }
    public ConditionParseException(String message, Throwable cause) { super(message, cause); }
}
