package io.imiocode.subagent.trace;

import io.imiocode.llm.TokenUsage;

import java.util.OptionalLong;

public record TraceTokenUsage(long input, long output, long reasoning, long cacheRead, long cacheWrite) {
    public static TraceTokenUsage zero() { return new TraceTokenUsage(0, 0, 0, 0, 0); }
    public TraceTokenUsage plus(TokenUsage usage) {
        return new TraceTokenUsage(input + value(usage.inputTokens()), output + value(usage.outputTokens()),
                reasoning + value(usage.reasoningTokens()), cacheRead + value(usage.cacheReadTokens()),
                cacheWrite + value(usage.cacheWriteTokens()));
    }
    private static long value(OptionalLong value) { return value.orElse(0); }
}
