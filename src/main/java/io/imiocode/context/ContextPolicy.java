package io.imiocode.context;

/** ch8 固定上下文管理策略。 */
public final class ContextPolicy {
    public static final double CHARACTERS_PER_TOKEN = 3.5d;
    public static final int SINGLE_RESULT_SPILL_CHARS = 5_000;
    public static final int AGGREGATE_RESULT_SPILL_CHARS = 20_000;
    public static final int OLD_RESULT_PREVIEW_CHARS = 2_000;
    public static final int RECENT_TOOL_MESSAGES_TO_KEEP = 3;
    public static final int SUMMARY_OUTPUT_TOKENS = 4_096;
    public static final int MAX_CONSECUTIVE_SUMMARY_FAILURES = 3;
    public static final int MAX_SUMMARY_RESPONSE_CHARS = 64_000;

    private ContextPolicy() {
    }
}
