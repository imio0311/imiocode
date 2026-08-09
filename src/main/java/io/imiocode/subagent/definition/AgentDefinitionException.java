package io.imiocode.subagent.definition;

public final class AgentDefinitionException extends RuntimeException {
    public AgentDefinitionException(String message) { super(message, null, false, false); }
    public AgentDefinitionException(String message, Throwable cause) { super(message, cause, false, false); }
}
