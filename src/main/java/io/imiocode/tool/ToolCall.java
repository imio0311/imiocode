package io.imiocode.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/** 已完成协议校验、可以交给执行器的工具调用。 */
public record ToolCall(String id, String name, ObjectNode arguments) {
    public ToolCall {
        id = requireText(id, "id");
        name = requireText(name, "name");
        arguments = Objects.requireNonNull(arguments, "arguments").deepCopy();
    }

    @Override
    public ObjectNode arguments() {
        return arguments.deepCopy();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
