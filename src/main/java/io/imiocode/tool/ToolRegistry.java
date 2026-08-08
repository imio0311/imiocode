package io.imiocode.tool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 集中管理工具注册、启用、禁用和协议导出。 */
public final class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final Set<String> enabled = ConcurrentHashMap.newKeySet();

    public synchronized void register(Tool tool) {
        Tool checked = Objects.requireNonNull(tool, "tool");
        String name = checked.definition().name();
        if (tools.containsKey(name)) {
            throw new IllegalArgumentException("工具已注册: " + name);
        }
        tools.put(name, checked);
        enabled.add(name);
    }

    /** 仅用于任务级动态工具；名称冲突时拒绝，避免覆盖已有能力。 */
    public synchronized void registerTemporary(Tool tool) {
        register(tool);
    }

    /** 移除任务级动态工具；静态工具调用方不应使用此方法。 */
    public synchronized Optional<Tool> unregister(String name) {
        if (name == null) return Optional.empty();
        enabled.remove(name);
        return Optional.ofNullable(tools.remove(name));
    }

    public synchronized void enable(String name) {
        requireRegistered(name);
        enabled.add(name);
    }

    public synchronized void disable(String name) {
        requireRegistered(name);
        enabled.remove(name);
    }

    public synchronized Optional<Tool> findEnabled(String name) {
        if (name == null || !enabled.contains(name)) {
            return Optional.empty();
        }
        return Optional.ofNullable(tools.get(name));
    }

    public synchronized Optional<Tool> findEnabled(String name, ToolSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (!selection.allows(name)) {
            return Optional.empty();
        }
        return findEnabled(name);
    }

    public synchronized ToolResolution resolve(String name, ToolSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (name == null || !tools.containsKey(name)) {
            return ToolResolution.unavailable(ToolAvailability.UNKNOWN);
        }
        if (!enabled.contains(name)) {
            return ToolResolution.unavailable(ToolAvailability.DISABLED);
        }
        if (!selection.allows(name)) {
            return ToolResolution.unavailable(ToolAvailability.DISALLOWED);
        }
        return ToolResolution.available(tools.get(name));
    }

    public synchronized Set<String> enabledNames() {
        return Set.copyOf(enabled);
    }

    public synchronized Set<String> registeredNames() {
        return Set.copyOf(tools.keySet());
    }

    public synchronized boolean isEnabled(String name) {
        return name != null && enabled.contains(name);
    }

    public synchronized List<ToolDefinition> enabledDefinitions() {
        return tools.entrySet().stream()
                .filter(entry -> enabled.contains(entry.getKey()))
                .map(entry -> entry.getValue().definition())
                .sorted(Comparator.comparing(ToolDefinition::name))
                .toList();
    }

    public synchronized List<ToolDefinition> enabledDefinitions(
            ToolSelection selection
    ) {
        Objects.requireNonNull(selection, "selection");
        return enabledDefinitions().stream()
                .filter(definition -> selection.allows(definition.name()))
                .toList();
    }

    public <T> List<T> exportEnabled(ToolDefinitionEncoder<T> encoder) {
        return exportEnabled(ToolSelection.allEnabled(), encoder);
    }

    public <T> List<T> exportEnabled(
            ToolSelection selection,
            ToolDefinitionEncoder<T> encoder) {
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(encoder, "encoder");
        List<T> encoded = new ArrayList<>();
        for (ToolDefinition definition : enabledDefinitions(selection)) {
            encoded.add(Objects.requireNonNull(encoder.encode(definition), "编码结果"));
        }
        return List.copyOf(encoded);
    }

    private void requireRegistered(String name) {
        if (name == null || !tools.containsKey(name)) {
            throw new IllegalArgumentException("未知工具: " + name);
        }
    }
}
