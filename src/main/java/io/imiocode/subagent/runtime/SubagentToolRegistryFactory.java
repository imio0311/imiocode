package io.imiocode.subagent.runtime;

import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLifecycleListener;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
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
}
