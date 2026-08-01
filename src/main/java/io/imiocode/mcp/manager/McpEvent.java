package io.imiocode.mcp.manager;

/** 不携带 env、Header 或工具参数的 MCP 事件。 */
public record McpEvent(
        McpEventType type,
        String serverName,
        String toolName,
        String safeMessage) {
    private static final int MAX_MESSAGE_LENGTH = 300;

    public McpEvent {
        if (type == null) {
            throw new IllegalArgumentException("type 不能为空");
        }
        serverName = serverName == null ? "" : serverName;
        toolName = toolName == null ? "" : toolName;
        safeMessage = safeMessage == null ? "" : safeMessage;
        if (safeMessage.length() > MAX_MESSAGE_LENGTH) {
            safeMessage = safeMessage.substring(0, MAX_MESSAGE_LENGTH) + "…";
        }
    }
}
