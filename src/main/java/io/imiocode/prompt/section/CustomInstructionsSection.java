package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

import java.util.Objects;

/** 调用方显式提供的稳定自定义指令。 */
public final class CustomInstructionsSection implements PromptSection {
    private final String content;

    public CustomInstructionsSection(String content) {
        this.content = Objects.requireNonNull(content, "自定义指令不能为空");
    }

    @Override
    public Section section() {
        return new Section(
                "自定义指令",
                SectionPriority.CUSTOM_INSTRUCTIONS,
                content);
    }
}
