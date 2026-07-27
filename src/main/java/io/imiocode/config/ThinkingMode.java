package io.imiocode.config;

import java.util.Locale;

public enum ThinkingMode {
    AUTO,
    ADAPTIVE,
    MANUAL;

    public static ThinkingMode parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置项 thinking.mode / IMIO_THINKING_MODE 无效");
        }
    }
}
