package io.imiocode.config;

import io.imiocode.mcp.config.McpConfigLoadResult;
import io.imiocode.permission.PermissionSettings;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.skill.install.SkillInstallConfig;
import io.imiocode.hook.config.HookConfigLoadResult;
import io.imiocode.subagent.config.SubagentConfig;
import io.imiocode.worktree.config.WorktreeConfig;
import io.imiocode.team.config.TeamRuntimeConfig;

import java.util.List;
import java.util.Objects;

/** 应用启动后各模块共用的不可变配置快照。 */
public record RuntimeConfig(
        AppConfig app,
        McpConfigLoadResult mcp,
        PermissionSettings permissions,
        SkillInstallConfig skillInstall,
        WorktreeConfig worktrees,
        SubagentConfig subagents,
        TeamRuntimeConfig teams,
        HookConfigLoadResult hooks,
        SecretRedactor redactor,
        ConfigSourceSummary sources,
        List<ConfigNotice> notices) {

    public RuntimeConfig {
        Objects.requireNonNull(app, "app");
        Objects.requireNonNull(mcp, "mcp");
        Objects.requireNonNull(permissions, "permissions");
        Objects.requireNonNull(skillInstall, "skillInstall");
        Objects.requireNonNull(worktrees, "worktrees");
        Objects.requireNonNull(subagents, "subagents");
        Objects.requireNonNull(teams, "teams");
        Objects.requireNonNull(hooks, "hooks");
        Objects.requireNonNull(redactor, "redactor");
        Objects.requireNonNull(sources, "sources");
        notices = List.copyOf(Objects.requireNonNullElse(notices, List.of()));
    }

    @Override
    public String toString() {
        return "RuntimeConfig[app=" + app
                + ", mcpServers=" + mcp.servers().size()
                + ", mcpErrors=" + mcp.errors().size()
                + ", permissionsMode=" + permissions.mode()
                + ", skillInstallHosts=" + skillInstall.allowedHosts().size()
                + ", worktreeDirectory=" + worktrees.directory()
                + ", subagentAliases=" + subagents.modelAliases().size()
                + ", teamBackend=" + teams.preferredBackend().configValue()
                + ", hooks=" + hooks.hooks().size()
                + ", hookErrors=" + hooks.errors().size()
                + ", redactor=***"
                + ", sources=" + sources
                + ", notices=" + notices.size() + "]";
    }
}
