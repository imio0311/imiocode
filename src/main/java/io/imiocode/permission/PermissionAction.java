package io.imiocode.permission;

import java.util.Locale;

/** 权限规则或检查器给出的动作。 */
public enum PermissionAction {
    ALLOW,
    ASK,
    DENY;

    public static PermissionAction parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("权限动作不能为空");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("未知权限动作: " + value);
        }
    }
}
