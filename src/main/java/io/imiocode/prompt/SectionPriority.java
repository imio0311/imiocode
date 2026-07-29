package io.imiocode.prompt;

/** System Prompt 固定模块的稳定优先级。 */
public enum SectionPriority {
    IDENTITY(100),
    BEHAVIOR(200),
    TOOL_USAGE(300),
    CODE_QUALITY(400),
    SECURITY(500),
    TASK_PATTERN(600),
    OUTPUT_STYLE(700),
    CUSTOM_INSTRUCTIONS(800),
    SKILL(900),
    MEMORY(950);

    private final int value;

    SectionPriority(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
