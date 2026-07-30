package io.imiocode.permission;

import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolRisk;

import java.util.Objects;

/** 一次工具调用对应的规范化权限请求。 */
public record PermissionRequest(
        ToolCall call,
        ToolRisk risk,
        PermissionOperation operation,
        String normalizedTarget,
        String displayTarget
) {
    public PermissionRequest {
        call = Objects.requireNonNull(call, "call 不能为空");
        risk = Objects.requireNonNull(risk, "risk 不能为空");
        operation = Objects.requireNonNull(operation, "operation 不能为空");
        normalizedTarget = requireText(normalizedTarget, "normalizedTarget");
        displayTarget = requireText(displayTarget, "displayTarget");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
