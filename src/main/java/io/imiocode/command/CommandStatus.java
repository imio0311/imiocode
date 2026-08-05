package io.imiocode.command;

import io.imiocode.agent.AgentMode;
import io.imiocode.permission.PermissionMode;
import io.imiocode.session.SessionId;

import java.nio.file.Path;
import java.util.Objects;

/** 命令和状态栏共享的无秘密运行状态快照。 */
public record CommandStatus(
        String provider,
        String model,
        Path workspace,
        AgentMode agentMode,
        PermissionMode permissionMode,
        SessionId session,
        long estimatedTokens,
        long contextWindowTokens,
        int connectedMcpServers,
        int registeredMcpTools
) {
    public CommandStatus {
        provider = requireText(provider, "Provider");
        model = requireText(model, "模型");
        workspace = Objects.requireNonNull(workspace, "工作目录不能为空").toAbsolutePath().normalize();
        agentMode = Objects.requireNonNull(agentMode, "Agent 模式不能为空");
        permissionMode = Objects.requireNonNull(permissionMode, "权限模式不能为空");
        session = Objects.requireNonNull(session, "会话不能为空");
        if (estimatedTokens < 0) throw new IllegalArgumentException("Token 估算不能为负数");
        if (contextWindowTokens <= 0) throw new IllegalArgumentException("上下文窗口必须为正数");
        if (connectedMcpServers < 0 || registeredMcpTools < 0) {
            throw new IllegalArgumentException("MCP 计数不能为负数");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + "不能为空");
        return value.trim();
    }
}
