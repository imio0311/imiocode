package io.imiocode.mcp.client;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** MCP Server 暴露的远程工具定义。 */
public record McpRemoteTool(String name, String description, ObjectNode inputSchema) {
    public McpRemoteTool {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("远程工具 name 不能为空");
        }
        name = name.trim();
        description = description == null || description.isBlank()
                ? "MCP 远程工具 " + name
                : description.trim();
        if (inputSchema == null) {
            inputSchema = JsonNodeFactory.instance.objectNode();
            inputSchema.put("type", "object");
            inputSchema.putObject("properties");
        } else {
            inputSchema = inputSchema.deepCopy();
        }
    }

    @Override
    public ObjectNode inputSchema() {
        return inputSchema.deepCopy();
    }
}
