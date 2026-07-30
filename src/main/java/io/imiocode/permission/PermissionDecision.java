package io.imiocode.permission;

import java.util.Objects;

/** 可安全展示原因的权限决策。 */
public record PermissionDecision(
        PermissionAction action,
        PermissionDecisionSource source,
        String reason
) {
    public PermissionDecision {
        action = Objects.requireNonNull(action, "action 不能为空");
        source = Objects.requireNonNull(source, "source 不能为空");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason 不能为空");
        }
        reason = reason.trim();
    }

    public static PermissionDecision allow(PermissionDecisionSource source, String reason) {
        return new PermissionDecision(PermissionAction.ALLOW, source, reason);
    }

    public static PermissionDecision ask(PermissionDecisionSource source, String reason) {
        return new PermissionDecision(PermissionAction.ASK, source, reason);
    }

    public static PermissionDecision deny(PermissionDecisionSource source, String reason) {
        return new PermissionDecision(PermissionAction.DENY, source, reason);
    }
}
