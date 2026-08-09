package io.imiocode.permission;

import java.util.Locale;

/** 从完全不信任到完全信任的会话权限模式。 */
public enum PermissionMode {
    LOCKDOWN,
    READ_ONLY,
    ASK,
    AUTO_EDIT,
    FULL_ACCESS;

    public static PermissionMode parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO_EDIT;
        }
        try {
            return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("未知权限模式: " + value);
        }
    }
}
