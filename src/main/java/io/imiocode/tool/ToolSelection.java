package io.imiocode.tool;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** 单次模型请求及对应工具执行共享的工具可见性快照。 */
public record ToolSelection(boolean unrestricted, Set<String> allowedNames) {
    public ToolSelection {
        Objects.requireNonNull(allowedNames, "allowedNames");
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        for (String name : allowedNames) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("工具选择名称不能为空");
            }
            copy.add(name.trim());
        }
        allowedNames = Set.copyOf(copy);
        if (unrestricted && !allowedNames.isEmpty()) {
            throw new IllegalArgumentException("unrestricted 工具选择不能同时指定名称");
        }
    }

    public static ToolSelection allEnabled() {
        return new ToolSelection(true, Set.of());
    }

    public static ToolSelection only(Set<String> names) {
        return new ToolSelection(false, names);
    }

    public boolean allows(String name) {
        return name != null && (unrestricted || allowedNames.contains(name));
    }

    /** 多层策略求交集；用于子 Agent 在父级能力之上继续收窄权限。 */
    public ToolSelection intersect(ToolSelection other, Set<String> availableNames) {
        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(availableNames, "availableNames");
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String name : availableNames) {
            if (allows(name) && other.allows(name)) names.add(name);
        }
        return only(names);
    }

    public ToolSelection without(Set<String> deniedNames, Set<String> availableNames) {
        Objects.requireNonNull(deniedNames, "deniedNames");
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String name : availableNames) {
            if (allows(name) && !deniedNames.contains(name)) names.add(name);
        }
        return only(names);
    }
}
