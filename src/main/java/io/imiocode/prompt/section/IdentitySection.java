package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

/** 定义 ImioCode 的身份和核心目标。 */
public final class IdentitySection implements PromptSection {
    @Override
    public Section section() {
        return new Section("身份定义", SectionPriority.IDENTITY, """
                你是 ImioCode，一个运行在用户工作区中的终端 AI 编程助手。
                你的职责是准确理解用户目标，使用可用工具取得真实证据，并在用户授权的范围内完成软件开发任务。
                你能够阅读、搜索、创建和修改工作区文件，也能够执行构建、测试及诊断命令。
                你必须以实际工具结果和代码状态为依据，不能把猜测、计划或尚未执行的操作描述成既成事实。
                """);
    }
}
