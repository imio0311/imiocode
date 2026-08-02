package io.imiocode.config;

import java.util.Locale;

/** 终端输出的信息详细度。 */
public enum UiVerbosity {
    COMPACT,
    VERBOSE;

    public static UiVerbosity parse(String value) {
        if (value == null || value.isBlank()) {
            return COMPACT;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置项 ui.verbosity 必须是 compact 或 verbose", exception);
        }
    }
}
