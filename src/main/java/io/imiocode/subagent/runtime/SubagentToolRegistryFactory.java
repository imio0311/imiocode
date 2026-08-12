package io.imiocode.subagent.runtime;

import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLifecycleListener;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolSelection;
import io.imiocode.tool.core.BashTool;
import io.imiocode.tool.core.EditFileTool;
import io.imiocode.tool.core.GlobTool;
import io.imiocode.tool.core.GrepTool;
import io.imiocode.tool.core.ReadFileTool;
import io.imiocode.tool.core.WriteFileTool;
import io.imiocode.tool.workspace.WorkspacePolicy;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** 为子 Agent 替换所有与工作区绑定的核心工具。 */
public final class SubagentToolRegistryFactory {
    private final ToolRegistry parent;
    private final ToolLimits limits;
    private final SecretRedactor redactor;

    public SubagentToolRegistryFactory(ToolRegistry parent, ToolLimits limits, SecretRedactor redactor) {
        this.parent = Objects.requireNonNull(parent); this.limits = Objects.requireNonNull(limits);
        this.redactor = Objects.requireNonNull(redactor);
    }

    public ToolRegistry create(Path workdir, ToolLifecycleListener lifecycle) {
        WorkspacePolicy policy = new WorkspacePolicy(workdir);
        ToolLifecycleListener listener = Objects.requireNonNullElse(lifecycle, ToolLifecycleListener.NOOP);
        return parent.copyWithReplacements(List.of(
                new ReadFileTool(policy, limits, redactor),
                new WriteFileTool(policy, limits, redactor, listener),
                new EditFileTool(policy, limits, redactor, listener),
                new BashTool(policy, limits, redactor, listener),
                new GlobTool(policy, limits, redactor),
                new GrepTool(policy, limits, redactor)));
    }

    /** 成员从父注册表复制工作工具，但硬移除 Lead/全局能力并绑定本成员团队工具。 */
    public ToolRegistry createTeamMember(Path workdir, ToolLifecycleListener lifecycle,
                                         java.util.Collection<? extends Tool> teamTools,
                                         ToolSelection selection) {
        ToolRegistry registry = create(workdir, lifecycle);
        for (Tool tool : teamTools) registry.register(tool);
        // 注册表本身也做物理裁剪，避免后续调用方误用 unrestricted selection 恢复主 Agent 能力。
        for (String name : registry.registeredNames()) {
            if (!selection.allows(name)) registry.unregister(name);
        }
        return registry;
    }
}
