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
    /** 无显式 tools 声明的团队成员只继承仓库内工作能力，不继承主 Agent 的扩展/全局能力。 */
    private static final Set<String> TEAM_MEMBER_DEFAULT_WORK_TOOLS = Set.of(
            "read_file", "write_file", "edit_file", "bash", "glob", "grep");
    private static final Set<String> TEAM_MEMBER_DENIED_TOOLS = Set.of(
            "agent", "TeamCreate", "TeamDelete", "TeamConverge",
            "CoordinatorMode", "CoordinatorAdvance", "install_skill", "load_skill",
            "TaskCreate", "TaskGet", "TaskList", "TaskUpdate", "TaskStop", "SendMessage");
    /** 一次性 SubAgent 永远不继承当前 Lead 的团队身份或团队控制面。 */
    private static final Set<String> ORDINARY_SUBAGENT_TEAM_TOOLS = Set.of(
            "TeamCreate", "TeamDelete", "TeamConverge", "CoordinatorMode", "CoordinatorAdvance",
            "TaskCreate", "TaskGet", "TaskList", "TaskUpdate", "TaskStop", "SendMessage");
    private final SubagentConfig config;

    public SubagentToolFilter(SubagentConfig config) { this.config = Objects.requireNonNull(config); }

    public ToolSelection select(AgentDefinition definition, Set<String> enabled, boolean background) {
        LinkedHashSet<String> result = new LinkedHashSet<>(enabled);
        result.removeAll(config.globallyDeniedTools());
        result.removeAll(ORDINARY_SUBAGENT_TEAM_TOOLS);
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

    /**
     * 团队成员的能力从显式白名单开始：未声明 tools 时仅给核心工作工具；
     * 声明 tools 时允许其显式列出的扩展工具，但永远移除 Lead/全局管理能力。
     * 团队 Task 与 SendMessage 由调用方按已绑定 principal 的实例单独加入。
     */
    public ToolSelection selectTeamMember(AgentDefinition definition, Set<String> enabled) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(enabled, "enabled");
        LinkedHashSet<String> candidates = new LinkedHashSet<>(enabled);
        if (definition.unrestrictedTools()) {
            candidates.retainAll(TEAM_MEMBER_DEFAULT_WORK_TOOLS);
        } else {
            candidates.retainAll(definition.tools());
        }
        candidates.removeAll(config.globallyDeniedTools());
        candidates.removeAll(TEAM_MEMBER_DENIED_TOOLS);
        candidates.removeAll(definition.disallowedTools());
        if (definition.permissionMode() == PermissionMode.READ_ONLY
                || definition.permissionMode() == PermissionMode.LOCKDOWN) {
            candidates.retainAll(READ_TOOLS);
        }
        if (definition.permissionMode() == PermissionMode.LOCKDOWN) candidates.clear();
        return ToolSelection.only(candidates);
    }
}
