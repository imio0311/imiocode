package io.imiocode.skill.install;

import io.imiocode.skill.SkillCatalogSnapshot;

@FunctionalInterface
public interface SkillInstallRefresher {
    SkillCatalogSnapshot reload();
}
