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

    public synchronized List<ToolDefinition> enabledDefinitions() {
        return tools.entrySet().stream()
                .filter(entry -> enabled.contains(entry.getKey()))
                .map(entry -> entry.getValue().definition())
                .sorted(Comparator.comparing(ToolDefinition::name))
                .toList();
    }

    public <T> List<T> exportEnabled(ToolDefinitionEncoder<T> encoder) {
        Objects.requireNonNull(encoder, "encoder");
        List<T> encoded = new ArrayList<>();
        for (ToolDefinition definition : enabledDefinitions()) {
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
