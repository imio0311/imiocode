package io.imiocode.mcp.manager;

import io.imiocode.mcp.config.McpConfigError;

import java.util.List;

/** MCP 启动的部分成功汇总。 */
public record McpStartupResult(
        int connectedServers,
        int registeredTools,
        List<McpConfigError> errors) {
    public McpStartupResult {
        if (connectedServers < 0 || registeredTools < 0) {
            throw new IllegalArgumentException("启动计数不能为负数");
        }
        errors = List.copyOf(errors == null ? List.of() : errors);
    }
}
