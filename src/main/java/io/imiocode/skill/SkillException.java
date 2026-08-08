package io.imiocode.skill;

/** 对用户安全的 Skill 配置或执行错误。 */
public final class SkillException extends RuntimeException {
    public SkillException(String message) {
        super(message);
    }

    public SkillException(String message, Throwable cause) {
        super(message, cause);
    }
}
