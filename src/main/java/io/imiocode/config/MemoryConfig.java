package io.imiocode.config;

/** 双层长期记忆与自动提取配置。 */
public record MemoryConfig(
        boolean enabled,
        boolean autoExtract,
        boolean userScopeEnabled,
        boolean projectScopeEnabled,
        int maxEntriesPerScope,
        int maxEntryChars,
        long maxFileBytes,
        int extractionOutputTokens) {
    public static final int HARD_MAX_ENTRIES = 2_000;
    public static final int HARD_MAX_ENTRY_CHARS = 4_000;
    public static final long HARD_MAX_FILE_BYTES = 2L * 1024L * 1024L;
    public static final int HARD_MAX_EXTRACTION_TOKENS = 4_096;

    public MemoryConfig {
        requireRange(maxEntriesPerScope, 1, HARD_MAX_ENTRIES, "memory.max-entries-per-scope");
        requireRange(maxEntryChars, 1, HARD_MAX_ENTRY_CHARS, "memory.max-entry-chars");
        if (maxFileBytes <= 0 || maxFileBytes > HARD_MAX_FILE_BYTES) {
            throw new IllegalArgumentException("memory.max-file-bytes 必须在 1 和 2097152 之间");
        }
        requireRange(extractionOutputTokens, 1, HARD_MAX_EXTRACTION_TOKENS,
                "memory.extraction-output-tokens");
    }

    public static MemoryConfig defaults() {
        return new MemoryConfig(true, false, true, true, 200, 1_000, 256L * 1024L, 1_024);
    }

    private static void requireRange(int value, int minimum, int maximum, String name) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " 必须在 " + minimum + " 和 " + maximum + " 之间");
        }
    }
}
