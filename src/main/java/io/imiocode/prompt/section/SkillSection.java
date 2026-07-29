package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

import java.util.Objects;

/** 调用方显式提供的稳定 Skill 文本。 */
public final class SkillSection implements PromptSection {
    private final String content;

    public SkillSection(String content) {
        this.content = Objects.requireNonNull(content, "Skill 内容不能为空");
    }

    @Override
    public Section section() {
        return new Section("Skill", SectionPriority.SKILL, content);
    }
}
