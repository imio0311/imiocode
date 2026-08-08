package io.imiocode.skill;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** 一次原子发布的 Skill 目录。 */
public record SkillCatalogSnapshot(
        long generation,
        Map<String, SkillDescriptor> skills,
        List<String> diagnostics
) {
    public SkillCatalogSnapshot {
        if (generation < 0) throw new IllegalArgumentException("generation 不能为负数");
        skills = Map.copyOf(Objects.requireNonNullElse(skills, Map.of()));
        diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
    }

    public Optional<SkillDescriptor> find(String name) {
        if (name == null) return Optional.empty();
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        SkillDescriptor direct = skills.get(normalized);
        if (direct != null) return Optional.of(direct);
        return skills.values().stream()
                .filter(value -> value.metadata().aliases().contains(normalized))
                .findFirst();
    }

    public List<SkillDescriptor> sorted() {
        return skills.values().stream()
                .sorted(Comparator.comparing(value -> value.metadata().name()))
                .toList();
    }

    public static SkillCatalogSnapshot empty() {
        return new SkillCatalogSnapshot(0, Map.of(), List.of());
    }
}
