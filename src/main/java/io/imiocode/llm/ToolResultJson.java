package io.imiocode.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolResult;

import java.util.Objects;

/** 将统一工具结果编码为三家协议共用的安全 JSON。 */
public final class ToolResultJson {
    private final ObjectMapper objectMapper;
    private final SecretRedactor redactor;

    public ToolResultJson(ObjectMapper objectMapper, SecretRedactor redactor) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    public ObjectNode encode(ToolResult result) {
        Objects.requireNonNull(result, "result");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("success", result.success());
        node.put("output", redactor.redact(result.output()));
        node.put("error", redactor.redact(result.error()));
        node.put("truncated", result.truncated());
        node.put("duration_ms", result.duration().toMillis());
        if (result.exitCode() != null) {
            node.put("exit_code", result.exitCode());
        }
        return node;
    }

    public String encodeString(ToolResult result) {
        try {
            return objectMapper.writeValueAsString(encode(result));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("无法编码工具结果");
        }
    }
}
