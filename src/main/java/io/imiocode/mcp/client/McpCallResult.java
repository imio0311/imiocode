package io.imiocode.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.List;

/** MCP tools/call 的协议级结果。 */
public record McpCallResult(
        List<JsonNode> content,
        JsonNode structuredContent,
        boolean error) {
    public McpCallResult {
        content = content == null
                ? List.of()
                : content.stream().map(node -> (JsonNode) node.deepCopy()).toList();
        structuredContent = structuredContent == null
                ? NullNode.getInstance()
                : structuredContent.deepCopy();
    }

    @Override
    public List<JsonNode> content() {
        return content.stream().map(node -> (JsonNode) node.deepCopy()).toList();
    }

    @Override
    public JsonNode structuredContent() {
        return structuredContent.deepCopy();
    }
}
