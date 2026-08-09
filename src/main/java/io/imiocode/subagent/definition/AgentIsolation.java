package io.imiocode.subagent.definition;

import java.util.Locale;

public enum AgentIsolation {
    NONE, WORKTREE;

    public static AgentIsolation parse(String value) {
        if (value == null || value.isBlank()) return NONE;
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_')); }
        catch (IllegalArgumentException exception) {
            throw new AgentDefinitionException("未知 isolation: " + value.trim());
        }
    }
}
