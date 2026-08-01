package io.imiocode.mcp.config;

import java.util.Locale;

/** MCP Server 使用的传输类型。 */
public enum McpTransportType {
    STDIO,
    STREAMABLE_HTTP;

    public static McpTransportType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("transport 不能为空");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "stdio" -> STDIO;
            case "streamable-http", "streamable_http", "http" -> STREAMABLE_HTTP;
            default -> throw new IllegalArgumentException("未知 transport");
        };
    }
}
