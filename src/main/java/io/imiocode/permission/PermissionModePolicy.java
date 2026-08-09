package io.imiocode.permission;

import java.util.Objects;

/** 五种权限模式的上限和默认行为。 */
public final class PermissionModePolicy {
    public PermissionDecision ceiling(PermissionRequest request, PermissionMode mode) {
        Objects.requireNonNull(request, "request 不能为空");
        Objects.requireNonNull(mode, "mode 不能为空");
        if (mode == PermissionMode.LOCKDOWN) {
            return PermissionDecision.deny(
                    PermissionDecisionSource.MODE, "LOCKDOWN 模式禁止全部工具");
        }
        if (mode == PermissionMode.READ_ONLY
                && request.operation() != PermissionOperation.READ) {
            return PermissionDecision.deny(
                    PermissionDecisionSource.MODE, "READ_ONLY 模式禁止写入和命令工具");
        }
        return null;
    }

    public PermissionDecision evaluate(PermissionRequest request, PermissionMode mode) {
        Objects.requireNonNull(request, "request 不能为空");
        Objects.requireNonNull(mode, "mode 不能为空");
        PermissionDecision ceiling = ceiling(request, mode);
        if (ceiling != null) {
            return ceiling;
        }
        return switch (mode) {
            case LOCKDOWN -> throw new IllegalStateException("LOCKDOWN 已由模式上限处理");
            case READ_ONLY -> PermissionDecision.allow(
                    PermissionDecisionSource.MODE, "READ_ONLY 模式允许只读工具");
            case ASK -> request.operation() == PermissionOperation.READ
                    ? PermissionDecision.allow(
                    PermissionDecisionSource.MODE, "ASK 模式自动允许只读工具")
                    : PermissionDecision.ask(
                    PermissionDecisionSource.MODE, "ASK 模式需要用户确认");
            case AUTO_EDIT -> request.risk() == io.imiocode.tool.ToolRisk.HIGH
                    ? PermissionDecision.ask(
                    PermissionDecisionSource.MODE, request.riskReason())
                    : PermissionDecision.allow(
                    PermissionDecisionSource.MODE, request.riskReason());
            case FULL_ACCESS -> PermissionDecision.allow(
                    PermissionDecisionSource.MODE, "FULL_ACCESS 模式允许普通操作");
        };
    }
}
