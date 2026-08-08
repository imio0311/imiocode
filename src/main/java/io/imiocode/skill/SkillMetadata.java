package io.imiocode.skill;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** 启动阶段常驻内存的 Skill 元信息，不包含正文。 */
public record SkillMetadata(
        String name,
        String description,
        SkillMode mode,
        SkillHistoryMode history,
        Set<String> aliases,
        Set<String> allowedTools
) {
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9-]{0,63}");
    private static final Pattern TOOL = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    public SkillMetadata {
        name = normalizeName(name, "Skill 名称");
        description = requireText(description, "Skill 描述");
        mode = Objects.requireNonNullElse(mode, SkillMode.INLINE);
        history = Objects.requireNonNullElse(history, SkillHistoryMode.RECENT);
        aliases = normalizeNames(aliases, name);
        allowedTools = normalizeTools(allowedTools);
    }

    private static Set<String> normalizeNames(Set<String> values, String name) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Set<String> source = values == null ? Set.of() : values;
        for (String value : source) {
            String normalized = normalizeName(value, "Skill 别名");
            if (normalized.equals(name)) {
                throw new SkillException("Skill 别名不能与名称相同: " + normalized);
            }
            if (!result.add(normalized)) {
                throw new SkillException("Skill 别名重复: " + normalized);
            }
        }
        return Set.copyOf(result);
    }

    private static Set<String> normalizeTools(Set<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Set<String> source = values == null ? Set.of() : values;
        for (String value : source) {
            String normalized = requireText(value, "工具名称");
            if (!TOOL.matcher(normalized).matches()) {
                throw new SkillException("Skill 工具名称格式无效: " + normalized);
            }
            result.add(normalized);
        }
        return Set.copyOf(result);
    }

    private static String normalizeName(String value, String label) {
        String normalized = requireText(value, label).toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (!NAME.matcher(normalized).matches()) {
            throw new SkillException(label + "格式无效: " + value);
        }
        return normalized;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new SkillException(label + "不能为空");
        return value.trim();
    }
}
