package io.imiocode.terminal;

import io.imiocode.llm.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 只展示 Provider 实际返回的 Token 用量字段，缺失字段不以零伪造。 */
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
