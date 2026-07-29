package io.imiocode.prompt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 按稳定优先级组装 System Prompt。 */
public final class SystemPromptBuilder {
    private static final Comparator<Section> SECTION_ORDER =
            Comparator.comparingInt((Section value) -> value.priority().value())
                    .thenComparing(Section::name);

    private final List<PromptSection> sections;

    public SystemPromptBuilder(List<PromptSection> sections) {
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Prompt Section 列表不能为空");
        }
        this.sections = List.copyOf(sections);
        this.sections.forEach(value -> Objects.requireNonNull(value, "Prompt Section 不能为空"));
    }

    public String build() {
        List<Section> resolved = new ArrayList<>(sections.size());
        Set<String> names = new HashSet<>();
        for (PromptSection provider : sections) {
            Section section = Objects.requireNonNull(provider.section(), "Section 结果不能为空");
            if (!names.add(section.name())) {
                throw new IllegalArgumentException("Prompt Section 名称重复: " + section.name());
            }
            if (!section.content().isBlank()) {
                resolved.add(section);
            }
        }
        resolved.sort(SECTION_ORDER);
        if (resolved.isEmpty()) {
            throw new IllegalStateException("System Prompt 不能没有有效内容");
        }
        return resolved.stream()
                .map(section -> "## " + section.name() + "\n" + section.content())
                .reduce((left, right) -> left + "\n\n" + right)
                .orElseThrow();
    }
}
