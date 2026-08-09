package io.imiocode.subagent.definition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record AgentCatalogSnapshot(long generation, Map<String, AgentDefinition> definitions,
                                   List<String> diagnostics) {
    public AgentCatalogSnapshot {
        if (generation < 0) throw new IllegalArgumentException("generation 不能为负数");
        definitions = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(definitions, Map.of())));
        diagnostics = List.copyOf(Objects.requireNonNullElse(diagnostics, List.of()));
    }
    public Optional<AgentDefinition> find(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(definitions.get(name.trim().toLowerCase()));
    }
    public static AgentCatalogSnapshot empty() { return new AgentCatalogSnapshot(0, Map.of(), List.of()); }
}
