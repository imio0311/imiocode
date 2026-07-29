package io.imiocode.prompt;

import io.imiocode.prompt.section.BehaviorSection;
import io.imiocode.prompt.section.CodeQualitySection;
import io.imiocode.prompt.section.CustomInstructionsSection;
import io.imiocode.prompt.section.IdentitySection;
import io.imiocode.prompt.section.MemorySection;
import io.imiocode.prompt.section.OutputStyleSection;
import io.imiocode.prompt.section.SecuritySection;
import io.imiocode.prompt.section.SkillSection;
import io.imiocode.prompt.section.TaskPatternSection;
import io.imiocode.prompt.section.ToolUsageSection;

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

    public SystemPromptBuilder() {
        this.sections = new ArrayList<>();
    }

    public SystemPromptBuilder(List<PromptSection> sections) {
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Prompt Section 列表不能为空");
        }
        this.sections = new ArrayList<>(List.copyOf(sections));
        this.sections.forEach(value ->
                Objects.requireNonNull(value, "Prompt Section 不能为空"));
    }

    public static SystemPromptBuilder defaults() {
        return defaults(BuildOptions.empty());
    }

    public static SystemPromptBuilder defaults(BuildOptions options) {
        BuildOptions checked = Objects.requireNonNull(
                options, "BuildOptions 不能为空");
        SystemPromptBuilder builder = new SystemPromptBuilder()
                .add(new IdentitySection())
                .add(new BehaviorSection())
                .add(new ToolUsageSection())
                .add(new CodeQualitySection())
                .add(new SecuritySection())
                .add(new TaskPatternSection())
                .add(new OutputStyleSection());
        checked.customInstructions()
                .map(CustomInstructionsSection::new)
                .ifPresent(builder::add);
        checked.skillContent()
                .map(SkillSection::new)
                .ifPresent(builder::add);
        checked.memoryContent()
                .map(MemorySection::new)
                .ifPresent(builder::add);
        return builder;
    }

    public synchronized SystemPromptBuilder add(PromptSection section) {
        sections.add(Objects.requireNonNull(
                section, "Prompt Section 不能为空"));
        return this;
    }

    public synchronized String build() {
        List<PromptSection> snapshot = List.copyOf(sections);
        List<Section> resolved = new ArrayList<>(snapshot.size());
        Set<String> names = new HashSet<>();
        for (PromptSection provider : snapshot) {
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
