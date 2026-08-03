package io.imiocode.config;

/** 项目指令发现与 include 展开的边界。 */
public record InstructionsConfig(boolean enabled, int maxIncludeDepth, long maxExpandedBytes) {
    public static final int DEFAULT_MAX_INCLUDE_DEPTH = 8;
    public static final long DEFAULT_MAX_EXPANDED_BYTES = 128L * 1024L;
    public static final int HARD_MAX_INCLUDE_DEPTH = 32;
    public static final long HARD_MAX_EXPANDED_BYTES = 1024L * 1024L;

    public InstructionsConfig {
        if (maxIncludeDepth <= 0 || maxIncludeDepth > HARD_MAX_INCLUDE_DEPTH) {
            throw new IllegalArgumentException("instructions.max-include-depth 必须在 1 和 32 之间");
        }
        if (maxExpandedBytes <= 0 || maxExpandedBytes > HARD_MAX_EXPANDED_BYTES) {
            throw new IllegalArgumentException("instructions.max-expanded-bytes 必须在 1 和 1048576 之间");
        }
    }

    public static InstructionsConfig defaults() {
        return new InstructionsConfig(true, DEFAULT_MAX_INCLUDE_DEPTH, DEFAULT_MAX_EXPANDED_BYTES);
    }
}
