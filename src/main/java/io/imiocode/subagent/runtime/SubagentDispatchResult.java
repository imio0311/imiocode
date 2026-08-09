package io.imiocode.subagent.runtime;

public record SubagentDispatchResult(boolean success, String message, String id, boolean background) {}
