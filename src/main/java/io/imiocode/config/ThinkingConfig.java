package io.imiocode.config;

import java.util.Objects;

public record ThinkingConfig(
        boolean enabled,
        ThinkingMode mode,
        int budgetTokens,
        ReasoningEffort effort,
        ReasoningSummary summary) {

    public ThinkingConfig {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(effort, "effort");
        Objects.requireNonNull(summary, "summary");
        if (budgetTokens <= 0) {
            throw new IllegalArgumentException("thinking.budget-tokens 必须为正数");
        }
    }

    public static ThinkingConfig disabled() {
        return new ThinkingConfig(false, ThinkingMode.AUTO, 1024, ReasoningEffort.HIGH, ReasoningSummary.AUTO);
    }
}
