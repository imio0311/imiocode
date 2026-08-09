package io.imiocode.subagent.runtime;

import io.imiocode.agent.AgentStopReason;
import io.imiocode.subagent.trace.TraceTokenUsage;

public record SubagentRunResult(boolean success, String output, AgentStopReason stopReason,
                                TraceTokenUsage usage, String traceId) {}
