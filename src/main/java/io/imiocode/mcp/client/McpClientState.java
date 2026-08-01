package io.imiocode.mcp.client;

/** MCP Client 生命周期状态。 */
public enum McpClientState {
    NEW,
    CONNECTING,
    READY,
    FAILED,
    CLOSED
}
