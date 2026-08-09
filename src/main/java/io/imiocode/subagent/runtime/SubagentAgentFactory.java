package io.imiocode.subagent.runtime;

import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.tool.ToolSelection;
import java.nio.file.Path;

@FunctionalInterface
public interface SubagentAgentFactory {
    SubagentAgentHandle create(AgentDefinition definition, ToolSelection selection);

    default SubagentAgentHandle create(AgentDefinition definition, ToolSelection selection, Path workdir) {
        return create(definition, selection);
    }
}
