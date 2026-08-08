package io.imiocode.skill;

import java.util.Objects;

/** 目录快照中的 Skill 描述符，不持有正文。 */
public record SkillDescriptor(
        SkillMetadata metadata,
        SkillOrigin origin,
        SkillSource source
) {
    public SkillDescriptor {
        metadata = Objects.requireNonNull(metadata, "metadata");
        origin = Objects.requireNonNull(origin, "origin");
        source = Objects.requireNonNull(source, "source");
    }
}
