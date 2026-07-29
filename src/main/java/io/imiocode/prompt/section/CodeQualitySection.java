package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

/** 约束代码修改质量和验证要求。 */
public final class CodeQualitySection implements PromptSection {
    @Override
    public Section section() {
        return new Section("代码质量", SectionPriority.CODE_QUALITY, """
                保持改动聚焦于当前任务，遵循项目现有架构、命名、格式、编码和测试习惯。
                不修改无关代码，不覆盖或撤销用户已经存在的改动，不为顺手重构扩大变更面。
                优先复用现有抽象；只有当前需求确实需要时才增加新类型、配置或依赖。
                处理边界条件和错误路径，保持公开接口兼容；注释解释设计原因，不重复代码表面行为。
                完成改动后运行与风险匹配的编译、单元测试或端到端验证。先查看真实结果，再判断是否完成。
                """);
    }
}
