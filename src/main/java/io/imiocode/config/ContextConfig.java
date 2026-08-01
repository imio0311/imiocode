package io.imiocode.config;

/** 上下文窗口与自动压缩阈值。 */
public record ContextConfig(int windowTokens, double autoCompactThreshold) {
    public static final int DEFAULT_WINDOW_TOKENS = 64_000;
    public static final double DEFAULT_AUTO_COMPACT_THRESHOLD = 0.80d;

    public ContextConfig {
        if (windowTokens <= 0) {
            throw new IllegalArgumentException("windowTokens 必须为正整数");
        }
        if (!Double.isFinite(autoCompactThreshold)
                || autoCompactThreshold <= 0d
                || autoCompactThreshold >= 1d) {
            throw new IllegalArgumentException("autoCompactThreshold 必须在 0 和 1 之间");
        }
    }

    public static ContextConfig defaults() {
        return new ContextConfig(DEFAULT_WINDOW_TOKENS, DEFAULT_AUTO_COMPACT_THRESHOLD);
    }
}
