package io.imiocode.mcp.manager;

/** MCP 生命周期事件监听器。 */
@FunctionalInterface
public interface McpEventListener {
    McpEventListener NOOP = event -> { };

    void onMcpEvent(McpEvent event);
}
