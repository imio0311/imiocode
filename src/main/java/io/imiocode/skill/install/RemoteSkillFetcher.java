package io.imiocode.skill.install;

public interface RemoteSkillFetcher {
    RemoteSkillPackage fetch(RemoteSkillLocation location, SkillDownloadBudget budget);
    void cancel();
}
