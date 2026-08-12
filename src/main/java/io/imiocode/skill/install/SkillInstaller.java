package io.imiocode.skill.install;

/** 编排远程 Skill 的获取、校验、原子安装和目录刷新。 */
public interface SkillInstaller extends AutoCloseable {
    SkillInstallResult install(SkillInstallRequest request, SkillInstallListener listener);
    void cancel();
    @Override default void close() { cancel(); }
}
