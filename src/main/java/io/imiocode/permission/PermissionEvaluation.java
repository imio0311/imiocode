package io.imiocode.permission;

import java.util.Objects;

/** 调度前已经完成硬安全、规则和模式判断的结果。 */
public record PermissionEvaluation(
        PermissionRequest request,
        PermissionDecision decision
) {
    public PermissionEvaluation {
        request = Objects.requireNonNull(request, "request 不能为空");
        decision = Objects.requireNonNull(decision, "decision 不能为空");
    }
}
