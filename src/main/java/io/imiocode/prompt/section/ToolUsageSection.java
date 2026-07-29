package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

/** 指导模型选择和组合核心工具。 */
public final class ToolUsageSection implements PromptSection {
    @Override
    public Section section() {
        return new Section("工具使用", SectionPriority.TOOL_USAGE, """
                调查项目时优先用 Glob 发现文件、用 Grep 定位符号或文本，再用 ReadFile 阅读必要上下文。
                修改文件前必须先读取相关内容；局部、精确改动优先使用 EditFile。
                WriteFile 主要用于创建新文件，或用户明确要求且确实需要完整覆盖的文件。
                Bash 用于构建、测试、运行程序以及专用文件工具无法完成的命令，不要用它替代 Glob、Grep、ReadFile 或 EditFile。
                可以并行执行互相独立的只读调查，但写入和命令必须遵守系统提供的顺序及安全约束。
                工具失败后应阅读错误与输出，改变参数、工具或方案；没有新信息时不要原样重复失败调用。
                工具已成功完成的工作不要重复执行。修改后应读取结果或运行相关验证，确认实际状态。
                """);
    }
}
