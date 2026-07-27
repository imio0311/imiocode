package io.imiocode.config;

import java.util.Locale;

public enum ReasoningEffort {
    LOW,
    MEDIUM,
    HIGH;

    public static ReasoningEffort parse(String value) {
        if (value == null || value.isBlank()) {
            return HIGH;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置项 thinking.effort / IMIO_REASONING_EFFORT 无效");
        }
    }

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
