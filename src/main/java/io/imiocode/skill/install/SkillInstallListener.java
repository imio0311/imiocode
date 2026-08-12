package io.imiocode.skill.install;

/** 接收已经脱敏、可以安全展示的 Skill 安装阶段通知。 */
@FunctionalInterface
public interface SkillInstallListener {
    SkillInstallListener NOOP = (stage, message) -> { };
    void onStage(SkillInstallStage stage, String safeMessage);
}
