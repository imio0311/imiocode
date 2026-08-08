package io.imiocode.skill.install;

/** 可安全展示给用户的远程 Skill 安装错误。 */
public final class SkillInstallException extends RuntimeException {
    public SkillInstallException(String message) {
        super(message);
    }

    public SkillInstallException(String message, Throwable cause) {
        super(message, cause);
    }
}
