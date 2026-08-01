package io.imiocode.mcp.client;

/** 初始化响应中的 Server 身份。 */
public record McpServerInfo(String name, String version) {
    public McpServerInfo {
        name = requireText(name, "name");
        version = requireText(version, "version");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
