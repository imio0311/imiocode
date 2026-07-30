package io.imiocode.permission;

import io.imiocode.tool.ToolRisk;

import java.util.Objects;

/** 供任意 UI 渲染的纯数据权限提示。 */
public record PermissionPrompt(
        String requestId,
        int iteration,
        String toolName,
        ToolRisk risk,
        String targetSummary,
        String reason
) {
    public PermissionPrompt {
        requestId = requireText(requestId, "requestId");
        if (iteration <= 0) {
            throw new IllegalArgumentException("iteration 必须大于 0");
        }
        toolName = requireText(toolName, "toolName");
        risk = Objects.requireNonNull(risk, "risk 不能为空");
        targetSummary = requireText(targetSummary, "targetSummary");
        reason = requireText(reason, "reason");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
