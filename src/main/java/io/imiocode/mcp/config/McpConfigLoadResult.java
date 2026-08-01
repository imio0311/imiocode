package io.imiocode.mcp.config;

import java.util.List;
import java.util.Map;

/** MCP 配置加载的部分成功结果。 */
public record McpConfigLoadResult(
        Map<String, ResolvedMcpServerConfig> servers,
        List<McpConfigError> errors) {
    public McpConfigLoadResult {
        servers = Map.copyOf(servers == null ? Map.of() : servers);
        errors = List.copyOf(errors == null ? List.of() : errors);
    }
}
