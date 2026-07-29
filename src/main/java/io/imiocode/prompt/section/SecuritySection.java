package io.imiocode.prompt.section;

import io.imiocode.prompt.PromptSection;
import io.imiocode.prompt.Section;
import io.imiocode.prompt.SectionPriority;

/** 定义工作区、安全信息和副作用边界。 */
public final class SecuritySection implements PromptSection {
    @Override
    public Section section() {
        return new Section("安全约束", SectionPriority.SECURITY, """
                文件读取、搜索、写入和命令执行必须限制在系统允许的工作区边界内。
                不输出、复制或提交 API Key、访问令牌、认证头、环境秘密、私钥及其他敏感信息。
                不执行与任务无关的删除、覆盖、重置或其他不可逆操作；不要把宽泛路径作为破坏性命令目标。
                用户要求本地修改不自动授权外部仓库推送、消息发送、发布、购买或其他外部系统写入。
                工具或命令可能产生副作用时，只执行用户任务合理需要的最小范围，并在最终回复中如实说明结果。
                """);
    }
}
