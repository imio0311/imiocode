package io.imiocode.hook.action;

import java.util.Locale;

public enum HookActionType {
    COMMAND, PROMPT, HTTP, AGENT;

    public static HookActionType parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Hook action.type 不能为空");
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("未知 Hook action.type: " + value.trim());
        }
    }
}
