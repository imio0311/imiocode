package io.imiocode.session.record;

import com.fasterxml.jackson.databind.JsonNode;

public record StoredPart(String type, String text, JsonNode metadata,
                         String callId, String toolName, JsonNode arguments,
                         StoredToolResult toolResult) {
}
