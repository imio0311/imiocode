package io.imiocode.subagent.filter;

import io.imiocode.permission.PermissionMode;
import io.imiocode.subagent.config.SubagentConfig;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.tool.ToolSelection;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** 按固定顺序叠加子 Agent 工具防线，后层只能收窄、不能恢复。 */
public final class SubagentToolFilter {
    private static final Set<String> READ_TOOLS = Set.of("read_file", "glob", "grep", "load_skill");
    private final SubagentConfig config;

    public SubagentToolFilter(SubagentConfig config) { this.config = Objects.requireNonNull(config); }

    public ToolSelection select(AgentDefinition definition, Set<String> enabled, boolean background) {
        LinkedHashSet<String> result = new LinkedHashSet<>(enabled);
        result.removeAll(config.globallyDeniedTools());
        if (background) result.retainAll(config.backgroundAllowedTools());
        if (!definition.unrestrictedTools()) result.retainAll(definition.tools());
        result.removeAll(definition.disallowedTools());
        if (definition.permissionMode() == PermissionMode.READ_ONLY
                || definition.permissionMode() == PermissionMode.LOCKDOWN) {
            result.retainAll(READ_TOOLS);
        }
        if (definition.permissionMode() == PermissionMode.LOCKDOWN) result.clear();
        return ToolSelection.only(result);
    }
}
