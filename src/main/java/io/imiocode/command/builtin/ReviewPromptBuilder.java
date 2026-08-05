package io.imiocode.command.builtin;

/** 构造稳定的代码审查 Prompt，避免把 Slash Command 语法写入会话。 */
public final class ReviewPromptBuilder {
    private static final String BASE = """
            请审查当前工作区的代码变更，只做审查，不要修改文件。
            先列出 findings，按严重度从高到低排序；每条必须包含文件位置、问题原因、影响和建议。
            重点检查正确性、安全性、回归风险、并发问题和缺失测试。
            如果没有发现问题，请明确说明，并指出仍存在的测试盲区或验证限制。
            """.strip();

    public String build(String focus) {
        if (focus == null || focus.isBlank()) return BASE;
        return BASE + "\n\nAdditional focus:\n" + focus.trim();
    }
}
