package io.imiocode.skill.install;

public interface SkillInstaller extends AutoCloseable {
    SkillInstallResult install(SkillInstallRequest request, SkillInstallListener listener);
    void cancel();
    @Override default void close() { cancel(); }
}
