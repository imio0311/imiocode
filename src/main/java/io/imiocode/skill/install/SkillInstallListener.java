package io.imiocode.skill.install;

@FunctionalInterface
public interface SkillInstallListener {
    SkillInstallListener NOOP = (stage, message) -> { };
    void onStage(SkillInstallStage stage, String safeMessage);
}
