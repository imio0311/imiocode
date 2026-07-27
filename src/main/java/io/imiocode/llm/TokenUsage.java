package io.imiocode.llm;

import java.util.Objects;
import java.util.OptionalLong;

public record TokenUsage(
        OptionalLong inputTokens,
        OptionalLong outputTokens,
        OptionalLong reasoningTokens,
        OptionalLong cacheReadTokens,
        OptionalLong cacheWriteTokens) {

    public TokenUsage {
        inputTokens = requireNonNegative(inputTokens, "inputTokens");
        outputTokens = requireNonNegative(outputTokens, "outputTokens");
        reasoningTokens = requireNonNegative(reasoningTokens, "reasoningTokens");
        cacheReadTokens = requireNonNegative(cacheReadTokens, "cacheReadTokens");
        cacheWriteTokens = requireNonNegative(cacheWriteTokens, "cacheWriteTokens");
    }

    public static TokenUsage unknown() {
        OptionalLong empty = OptionalLong.empty();
        return new TokenUsage(empty, empty, empty, empty, empty);
    }

    public boolean hasKnownValue() {
        return inputTokens.isPresent() || outputTokens.isPresent() || reasoningTokens.isPresent()
                || cacheReadTokens.isPresent() || cacheWriteTokens.isPresent();
    }

    private static OptionalLong requireNonNegative(OptionalLong value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isPresent() && value.getAsLong() < 0) {
            throw new IllegalArgumentException(name + " 不能为负数");
        }
        return value;
    }
}
