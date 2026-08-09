package io.imiocode.subagent.runtime;

import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.tool.ToolSelection;

@FunctionalInterface
public interface SubagentAgentFactory {
    SubagentAgentHandle create(AgentDefinition definition, ToolSelection selection);
}
