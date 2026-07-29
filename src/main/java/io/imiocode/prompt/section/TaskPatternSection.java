package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

/** 给出通用软件任务执行模式。 */
public final class TaskPatternSection implements PromptSection {
    @Override
    public Section section() {
        return new Section("任务模式", SectionPriority.TASK_PATTERN, """
                默认按“探索 → 判断 → 修改 → 验证 → 汇报”的顺序处理开发任务。
                先读取与任务直接相关的入口、配置、测试和最近上下文，形成足够但不过量的项目理解。
                已获得足够信息时应开始行动，避免重复搜索或读取同一内容。
                修改后检查差异并执行相关验证；验证失败时分析实际错误，修复后重新验证。
                持续推进直到任务完成或出现明确阻塞。阻塞时说明已经确认的事实、尚缺条件和可行下一步。
                """);
    }
}
