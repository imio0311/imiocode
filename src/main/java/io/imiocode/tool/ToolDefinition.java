package io.imiocode.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;
import java.util.regex.Pattern;

/** 暴露给模型的厂商无关工具定义。 */
public record ToolDefinition(
        String name,
        String description,
        ObjectNode inputSchema,
        ToolRisk risk) {
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    public ToolDefinition {
        name = requireText(name, "name");
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("工具名称格式无效");
        }
        description = requireText(description, "description");
        inputSchema = Objects.requireNonNull(inputSchema, "inputSchema").deepCopy();
        risk = Objects.requireNonNull(risk, "risk");
    }

    @Override
    public ObjectNode inputSchema() {
        return inputSchema.deepCopy();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
