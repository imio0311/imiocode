package io.imiocode.team.model;

import java.util.Locale;

public enum TeamBackend {
    AUTO("auto"), TMUX("tmux"), ITERM2("iterm2"), IN_PROCESS("in-process");
    private final String configValue;
    TeamBackend(String configValue) { this.configValue = configValue; }
    public String configValue() { return configValue; }
    public static TeamBackend parse(String value) {
        if (value == null || value.isBlank()) return AUTO;
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (TeamBackend backend : values()) if (backend.configValue.equals(normalized)) return backend;
        throw new IllegalArgumentException("未知团队后端: " + value);
    }
}
