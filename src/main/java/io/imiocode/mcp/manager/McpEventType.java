package io.imiocode.mcp.manager;

/** MCP 启动和生命周期的终端可观测事件。 */
public enum McpEventType {
    WAITING_FOR_APPROVAL,
    APPROVED,
    DENIED,
    CONNECTING,
    CONNECTED,
    TOOL_DISCOVERED,
    SERVER_FAILED,
    CLOSED
}
