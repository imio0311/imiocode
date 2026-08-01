package io.imiocode.mcp.manager;

import java.util.List;

/** stdio 启动确认所需的最小非敏感信息。 */
public record McpLaunchRequest(
        String serverName,
        String command,
        List<String> args) {
    public McpLaunchRequest {
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("serverName 不能为空");
        }
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command 不能为空");
        }
        serverName = serverName.trim();
        command = command.trim();
        args = List.copyOf(args == null ? List.of() : args);
    }
}
