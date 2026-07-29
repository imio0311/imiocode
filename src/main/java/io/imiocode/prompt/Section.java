package io.imiocode.prompt;

import java.util.Objects;

/** 一个可排序、可独立测试的 System Prompt 模块。 */
public record Section(
        String name,
        SectionPriority priority,
        String content
) {
    public Section {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Section 名称不能为空");
        }
        name = name.trim();
        priority = Objects.requireNonNull(priority, "Section 优先级不能为空");
        content = Objects.requireNonNull(content, "Section 内容不能为空").trim();
    }
}
