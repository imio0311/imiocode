package io.imiocode.mcp.config;

import java.nio.file.Path;
import java.util.Objects;

/** 不包含配置秘密的 MCP 加载错误。 */
public record McpConfigError(
        Path source,
        String serverName,
        String code,
        String safeMessage) {
    public McpConfigError {
        source = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
        serverName = serverName == null ? "" : serverName;
        code = requireText(code, "code");
        safeMessage = requireText(safeMessage, "safeMessage");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
