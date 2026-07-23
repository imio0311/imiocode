package io.imiocode.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolResultJsonTest {
    @Test
    void encodesAllFieldsAsParseableJsonAndRedactsSecrets() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ToolResult result = new ToolResult(
                false, "中文 \"test-key\"", "Bearer abc", true, Duration.ofMillis(12), 7);

        String encoded = new ToolResultJson(mapper, new SecretRedactor("test-key")).encodeString(result);
        JsonNode node = mapper.readTree(encoded);

        assertFalse(node.path("success").asBoolean());
        assertFalse(encoded.contains("test-key"));
        assertFalse(encoded.contains("Bearer abc"));
        assertTrue(node.path("truncated").asBoolean());
        assertEquals(12, node.path("duration_ms").asLong());
        assertEquals(7, node.path("exit_code").asInt());
    }

    @Test
    void omitsExitCodeForNonCommandTools() {
        JsonNode node = new ToolResultJson(new ObjectMapper(), new SecretRedactor(""))
                .encode(ToolResult.success("ok"));
        assertFalse(node.has("exit_code"));
    }
}
