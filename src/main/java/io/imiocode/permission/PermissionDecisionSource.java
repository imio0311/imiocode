package io.imiocode.permission;

/** 最终权限决策的来源。 */
public enum PermissionDecisionSource {
    DANGEROUS_COMMAND,
    SANDBOX,
    USER_RULE,
    PROJECT_RULE,
    LOCAL_RULE,
    SAFE_COMMAND,
    MODE,
    SESSION,
    USER,
    ERROR
}
