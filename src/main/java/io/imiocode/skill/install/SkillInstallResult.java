package io.imiocode.skill.install;

import io.imiocode.skill.SkillOrigin;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record SkillInstallResult(
        String skillName,
        Path installedPath,
        SkillOrigin origin,
        boolean replaced,
        List<SkillInstallStage> stages
) {
    public SkillInstallResult {
        if (skillName == null || skillName.isBlank()) throw new IllegalArgumentException("skillName 不能为空");
        installedPath = Objects.requireNonNull(installedPath, "installedPath").toAbsolutePath().normalize();
        origin = Objects.requireNonNull(origin, "origin");
        stages = List.copyOf(Objects.requireNonNullElse(stages, List.of()));
    }
}
