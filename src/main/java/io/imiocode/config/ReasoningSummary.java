package io.imiocode.config;

import java.util.Locale;

public enum ReasoningSummary {
    AUTO,
    CONCISE,
    DETAILED;

    public static ReasoningSummary parse(String value) {
        if (value == null || value.isBlank()) {
            return AUTO;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置项 thinking.summary / IMIO_REASONING_SUMMARY 无效");
        }
    }

    public String apiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
