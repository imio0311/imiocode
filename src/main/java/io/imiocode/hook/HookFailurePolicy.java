package io.imiocode.hook;

import java.util.Locale;

/** Hook 动作失败后的处理方式。 */
public enum HookFailurePolicy {
    IGNORE, FAIL, REJECT;

    public static HookFailurePolicy parse(String value) {
        if (value == null || value.isBlank()) return IGNORE;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("未知 Hook on-error: " + value.trim());
        }
    }
}
