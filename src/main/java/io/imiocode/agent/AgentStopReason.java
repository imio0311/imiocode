package io.imiocode.agent;

/**
 * Agent 停止执行的原因。
 */
public enum AgentStopReason {
    FINAL_RESPONSE,
    MAX_ITERATIONS,
    TIMEOUT,
    ERROR,
    CANCELLED,
    TOO_MANY_UNKNOWN_TOOLS
}
