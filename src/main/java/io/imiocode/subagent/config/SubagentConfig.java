package io.imiocode.subagent.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 子 Agent 的全局安全与容量配置。 */
public record SubagentConfig(
        Map<String, String> modelAliases,
        Set<String> globallyDeniedTools,
        Set<String> backgroundAllowedTools,
        int maxBackgroundTasks,
        int maxTaskRecords,
        int notificationCapacity) {

    public SubagentConfig {
        modelAliases = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(modelAliases, Map.of())));
        globallyDeniedTools = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNullElse(globallyDeniedTools, Set.of())));
        backgroundAllowedTools = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNullElse(backgroundAllowedTools, Set.of())));
        if (maxBackgroundTasks < 1 || maxBackgroundTasks > 32) throw new IllegalArgumentException("maxBackgroundTasks 必须在 1..32 之间");
        if (maxTaskRecords < 8) throw new IllegalArgumentException("maxTaskRecords 不能小于 8");
        if (notificationCapacity < 8) throw new IllegalArgumentException("notificationCapacity 不能小于 8");
    }

    public static SubagentConfig defaults() {
        return new SubagentConfig(Map.of(), Set.of("agent", "install_skill"),
                Set.of("read_file", "glob", "grep"), 4, 128, 128);
    }
}
