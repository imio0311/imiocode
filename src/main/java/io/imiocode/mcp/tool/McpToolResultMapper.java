package io.imiocode.mcp.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.mcp.client.McpCallResult;
import io.imiocode.tool.ToolResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 将 MCP content 映射为本地文本结果，不复制二进制正文。 */
public final class McpToolResultMapper {
    private final ObjectMapper mapper;

    public McpToolResultMapper() {
        this(new ObjectMapper());
    }

    McpToolResultMapper(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public ToolResult map(McpCallResult result) {
        Objects.requireNonNull(result, "result");
        List<String> parts = new ArrayList<>();
        for (JsonNode content : result.content()) {
            String type = content.path("type").asText("");
            switch (type) {
                case "text" -> parts.add(content.path("text").asText(""));
                case "image", "audio" -> parts.add(binarySummary(type, content));
                case "resource", "resource_link" -> parts.add(resourceSummary(type, content));
                default -> parts.add("[MCP 内容类型=" + safeType(type) + "，正文已省略]");
            }
        }
        if (!result.structuredContent().isNull()) {
            parts.add(prettyJson(result.structuredContent()));
        }
        String output = parts.stream()
                .filter(part -> part != null && !part.isBlank())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("无输出");
        return result.error()
                ? ToolResult.failure("", output, false, null)
                : ToolResult.success(output);
    }

    private static String binarySummary(String type, JsonNode content) {
        String mime = content.path("mimeType").asText("unknown");
        int encodedChars = content.path("data").isTextual()
                ? content.path("data").textValue().length()
                : 0;
        return "[" + type + " mimeType=" + mime
                + (encodedChars > 0 ? " encodedChars=" + encodedChars : "")
                + "，二进制正文已省略]";
    }

    private static String resourceSummary(String type, JsonNode content) {
        JsonNode resource = "resource".equals(type) ? content.path("resource") : content;
        String uri = resource.path("uri").asText("");
        String mime = resource.path("mimeType").asText("");
        return "[" + type
                + (uri.isBlank() ? "" : " uri=" + uri)
                + (mime.isBlank() ? "" : " mimeType=" + mime)
                + "，资源正文已省略]";
    }

    private String prettyJson(JsonNode value) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("无法序列化 structuredContent", exception);
        }
    }

    private static String safeType(String type) {
        if (type == null || type.isBlank()) {
            return "unknown";
        }
        return type.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
