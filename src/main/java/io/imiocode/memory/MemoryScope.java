package io.imiocode.memory;

import java.util.Locale;

public enum MemoryScope {
    USER, PROJECT;

    public static MemoryScope parse(String value) {
        if (value == null) throw new IllegalArgumentException("记忆作用域不能为空");
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("记忆作用域必须是 user 或 project"); }
    }
}
