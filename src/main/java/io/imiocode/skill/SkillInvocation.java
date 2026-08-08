package io.imiocode.skill;

import java.util.Objects;

/** 一次已经完成 fail-fast 校验的 Skill 调用。 */
public record SkillInvocation(LoadedSkill skill, String arguments) {
    public SkillInvocation {
        skill = Objects.requireNonNull(skill, "skill");
        arguments = arguments == null ? "" : arguments;
    }
}
