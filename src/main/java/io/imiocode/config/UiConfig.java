package io.imiocode.config;

import java.util.Objects;

/** 终端 UI 配置。 */
public record UiConfig(UiVerbosity verbosity) {
    public UiConfig {
        verbosity = Objects.requireNonNullElse(verbosity, UiVerbosity.COMPACT);
    }

    public static UiConfig defaults() {
        return new UiConfig(UiVerbosity.COMPACT);
    }
}
