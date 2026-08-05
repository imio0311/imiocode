package io.imiocode.command;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** 命令目录中可安全展示的静态元数据。 */
public record CommandDescriptor(
        String name,
        Set<String> aliases,
        String usage,
        String description,
        CommandType type,
        boolean compatibility
) {
    private static final Pattern NAME = Pattern.compile("(?:[a-z][a-z0-9-]*|\\?)");

    public CommandDescriptor {
        name = normalizeName(name, "命令名");
        usage = requireText(usage, "命令用法");
        description = requireText(description, "命令描述");
        type = Objects.requireNonNull(type, "命令类型不能为空");

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        Set<String> sourceAliases = aliases == null ? Set.of() : aliases;
        for (String alias : sourceAliases) {
            String value = normalizeName(alias, "命令别名");
            if (value.equals(name)) throw new IllegalArgumentException("命令别名不能与主名相同：" + value);
            if (!normalized.add(value)) throw new IllegalArgumentException("命令别名重复：" + value);
        }
        aliases = Set.copyOf(normalized);
    }

    public CommandDescriptor(
            String name,
            Set<String> aliases,
            String usage,
            String description,
            CommandType type) {
        this(name, aliases, usage, description, type, false);
    }

    private static String normalizeName(String value, String label) {
        String normalized = requireText(value, label).toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (!NAME.matcher(normalized).matches()) throw new IllegalArgumentException(label + "格式无效：" + value);
        return normalized;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + "不能为空");
        return value.trim();
    }
}
