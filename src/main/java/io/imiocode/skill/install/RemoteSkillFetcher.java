package io.imiocode.skill.install;

/** 在下载预算与来源限制内解析并获取远程 Skill 包。 */
public interface RemoteSkillFetcher {
    RemoteSkillPackage fetch(RemoteSkillLocation location, SkillDownloadBudget budget);
    void cancel();
}
