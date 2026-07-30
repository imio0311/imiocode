package io.imiocode.permission;

import java.util.Objects;
import java.util.Optional;

/** 一条保持原始书写顺序的分层权限规则。 */
public record PermissionRule(
        PermissionRuleLayer layer,
        PermissionAction action,
        String toolPattern,
        Optional<String> targetPattern
) {
    public PermissionRule {
        layer = Objects.requireNonNull(layer, "layer 不能为空");
        action = Objects.requireNonNull(action, "action 不能为空");
        if (toolPattern == null || toolPattern.isBlank()) {
            throw new IllegalArgumentException("toolPattern 不能为空");
        }
        toolPattern = toolPattern.trim();
        targetPattern = targetPattern == null
                ? Optional.empty()
                : targetPattern.map(String::trim);
        if (targetPattern.isPresent() && targetPattern.orElseThrow().isBlank()) {
            throw new IllegalArgumentException("targetPattern 不能为空字符串");
        }
    }
}
