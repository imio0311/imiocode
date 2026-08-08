package io.imiocode.skill;

/** Skill 来源；数值越大，覆盖优先级越高。 */
public enum SkillOrigin {
    BUILTIN(0),
    USER(1),
    PROJECT(2);

    private final int priority;

    SkillOrigin(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
