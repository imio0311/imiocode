package io.imiocode.terminal;

import io.imiocode.llm.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class UsageFormatter {
    public String format(TokenUsage usage) {
        Objects.requireNonNull(usage, "usage");
        List<String> fields = new ArrayList<>();
        usage.inputTokens().ifPresent(value -> fields.add("input=" + value));
        usage.outputTokens().ifPresent(value -> fields.add("output=" + value));
        usage.reasoningTokens().ifPresent(value -> fields.add("reasoning=" + value));
        usage.cacheReadTokens().ifPresent(value -> fields.add("cache-read=" + value));
        usage.cacheWriteTokens().ifPresent(value -> fields.add("cache-write=" + value));
        return fields.isEmpty() ? "" : "[usage] " + String.join(" · ", fields);
    }
}
