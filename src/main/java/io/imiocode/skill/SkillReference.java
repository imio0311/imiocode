package io.imiocode.skill;

/** 激活阶段加载的一份只读参考资料。 */
public record SkillReference(String path, String content) {
    public SkillReference {
        if (path == null || path.isBlank()) throw new SkillException("参考资料路径不能为空");
        content = content == null ? "" : content;
    }
}
