package io.imiocode.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.mcp.client.McpCallResult;
import io.imiocode.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolResultMapperTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final McpToolResultMapper resultMapper = new McpToolResultMapper();

    @Test
    void mapsTextStructuredAndEmptyResults() {
        var text1 = mapper.createObjectNode().put("type", "text").put("text", "first");
        var text2 = mapper.createObjectNode().put("type", "text").put("text", "second");
        ToolResult result = resultMapper.map(new McpCallResult(
                List.of(text1, text2),
                mapper.createObjectNode().put("count", 2),
                false));
        assertTrue(result.success());
        assertTrue(result.output().contains("first\nsecond"));
        assertTrue(result.output().contains("\"count\" : 2"));
        assertTrue(resultMapper.map(new McpCallResult(List.of(), null, false))
                .output().contains("无输出"));
    }

    @Test
    void omitsBinaryAndMapsBusinessError() {
        var image = mapper.createObjectNode()
                .put("type", "image")
                .put("mimeType", "image/png")
                .put("data", "VERY_SECRET_BASE64");
        ToolResult result = resultMapper.map(new McpCallResult(List.of(image), null, true));
        assertFalse(result.success());
        assertFalse(result.error().contains("VERY_SECRET_BASE64"));
        assertTrue(result.error().contains("二进制正文已省略"));
    }
}
