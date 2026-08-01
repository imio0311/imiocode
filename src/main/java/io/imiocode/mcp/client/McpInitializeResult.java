package io.imiocode.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.Objects;

/** MCP initialize 的协商结果。 */
public record McpInitializeResult(
        String protocolVersion,
        McpServerInfo serverInfo,
        JsonNode capabilities,
        String instructions) {
    public McpInitializeResult {
        if (protocolVersion == null || protocolVersion.isBlank()) {
            throw new IllegalArgumentException("protocolVersion 不能为空");
        }
        protocolVersion = protocolVersion.trim();
        serverInfo = Objects.requireNonNull(serverInfo, "serverInfo");
        capabilities = capabilities == null ? NullNode.getInstance() : capabilities.deepCopy();
        instructions = instructions == null ? "" : instructions;
    }

    @Override
    public JsonNode capabilities() {
        return capabilities.deepCopy();
    }
}
