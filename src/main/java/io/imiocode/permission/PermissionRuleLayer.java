package io.imiocode.permission;

/** 权限规则来源层，枚举顺序即匹配优先级。 */
public enum PermissionRuleLayer {
    USER,
    PROJECT,
    LOCAL
}
