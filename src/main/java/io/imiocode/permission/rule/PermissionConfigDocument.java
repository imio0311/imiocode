package io.imiocode.permission.rule;

import java.util.List;

/** 单个 permissions.yaml 的严格反序列化模型。 */
record PermissionConfigDocument(String mode, List<RuleDocument> rules) {
    PermissionConfigDocument {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    record RuleDocument(String action, String tool, String target) {
    }
}
