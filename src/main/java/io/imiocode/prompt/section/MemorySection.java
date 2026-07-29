package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

import java.util.Objects;

/** 调用方显式提供的稳定 Memory 文本。 */
public final class MemorySection implements PromptSection {
    private final String content;

    public MemorySection(String content) {
        this.content = Objects.requireNonNull(content, "Memory 内容不能为空");
    }

    @Override
    public Section section() {
        return new Section("Memory", SectionPriority.MEMORY, content);
    }
}
