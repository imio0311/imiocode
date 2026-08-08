package io.imiocode.skill;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 激活阶段完整加载且校验通过的 Skill。 */
public record LoadedSkill(
        SkillDescriptor descriptor,
        String prompt,
        List<SkillReference> references,
        List<SkillToolSpec> tools,
        Set<String> allowedTools
) {
    public LoadedSkill {
        descriptor = Objects.requireNonNull(descriptor, "descriptor");
        if (prompt == null || prompt.isBlank()) throw new SkillException("Skill 正文不能为空");
        prompt = prompt.trim();
        references = List.copyOf(Objects.requireNonNullElse(references, List.of()));
        tools = List.copyOf(Objects.requireNonNullElse(tools, List.of()));
        allowedTools = Set.copyOf(Objects.requireNonNullElse(allowedTools, Set.of()));
    }

    public SkillMetadata metadata() {
        return descriptor.metadata();
    }
}
