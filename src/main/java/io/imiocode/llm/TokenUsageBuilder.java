package io.imiocode.llm;

import java.util.OptionalLong;

public final class TokenUsageBuilder {
    private Long input;
    private Long output;
    private Long reasoning;
    private Long cacheRead;
    private Long cacheWrite;

    public TokenUsageBuilder input(long value) {
        input = checked(value);
        return this;
    }

    public TokenUsageBuilder output(long value) {
        output = checked(value);
        return this;
    }

    public TokenUsageBuilder reasoning(long value) {
        reasoning = checked(value);
        return this;
    }

    public TokenUsageBuilder cacheRead(long value) {
        cacheRead = checked(value);
        return this;
    }

    public TokenUsageBuilder cacheWrite(long value) {
        cacheWrite = checked(value);
        return this;
    }

    public TokenUsage build() {
        return new TokenUsage(optional(input), optional(output), optional(reasoning),
                optional(cacheRead), optional(cacheWrite));
    }

    private static long checked(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Token 用量不能为负数");
        }
        return value;
    }

    private static OptionalLong optional(Long value) {
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }
}
