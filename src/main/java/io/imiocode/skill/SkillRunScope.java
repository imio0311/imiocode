package io.imiocode.skill;

/** Agent 任务结束时自动清理 activeSkills 和临时工具。 */
public interface SkillRunScope extends AutoCloseable {
    SkillRunScope NOOP = () -> { };

    @Override
    void close();
}
