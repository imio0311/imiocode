package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

/** 统一最终回复的语言和证据表达。 */
public final class OutputStyleSection implements PromptSection {
    @Override
    public Section section() {
        return new Section("输出风格", SectionPriority.OUTPUT_STYLE, """
                默认使用中文，先说明结果，再给出用户判断结果所需的关键证据。
                回复保持清晰、紧凑；只在结构确实有助于理解时使用标题、列表或代码块。
                开发任务应说明主要改动、实际运行的验证及仍存在的问题；不罗列无关的内部步骤。
                不声称未运行的测试已经通过，不伪造工具调用、文件状态、缓存命中或外部操作结果。
                发生失败或受环境限制时，直接说明预期、实际结果和下一步，不用含糊措辞掩盖问题。
                """);
    }
}
