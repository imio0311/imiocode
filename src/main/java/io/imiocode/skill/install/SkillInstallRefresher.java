package io.imiocode.skill.install;

import io.imiocode.skill.SkillCatalogSnapshot;

/** 安装成功后重新加载 Skill 目录并返回最新快照。 */
@FunctionalInterface
public interface SkillInstallRefresher {
    SkillCatalogSnapshot reload();
}
