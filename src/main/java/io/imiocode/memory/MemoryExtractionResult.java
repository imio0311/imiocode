package io.imiocode.memory;

import java.util.List;

public record MemoryExtractionResult(List<MemoryCandidate> candidates, List<String> warnings) {
    public MemoryExtractionResult {
        candidates = List.copyOf(candidates == null ? List.of() : candidates);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
    }

    public static MemoryExtractionResult warning(String warning) {
        return new MemoryExtractionResult(List.of(), List.of(warning));
    }
}
