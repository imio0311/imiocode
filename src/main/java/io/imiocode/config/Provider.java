package io.imiocode.config;

import java.util.Locale;

public enum Provider {
    OPENAI,
    ANTHROPIC,
    DEEPSEEK;

    public static Provider parse(String value) {
        if (value == null || value.isBlank()) {
            throw new ConfigException("缺少配置项 IMIO_PROVIDER");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置项 IMIO_PROVIDER 必须是 openai、anthropic 或 deepseek", exception);
        }
    }

    public String configValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
