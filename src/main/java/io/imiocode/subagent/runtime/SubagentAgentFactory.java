package io.imiocode.subagent.runtime;

import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.tool.ToolSelection;
import java.nio.file.Path;

/** 根据 Agent 定义、裁剪后的工具集和可选隔离目录创建独立运行句柄。 */
@FunctionalInterface
public interface SubagentAgentFactory {
    SubagentAgentHandle create(AgentDefinition definition, ToolSelection selection);

    default SubagentAgentHandle create(AgentDefinition definition, ToolSelection selection, Path workdir) {
        // 兼容不需要独立工作目录的实现；Worktree 感知实现应覆盖此入口。
        return create(definition, selection);
    }
}
