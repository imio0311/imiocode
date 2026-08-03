package io.imiocode.memory;

import java.util.List;

public record MemoryUpdateReport(int added, int updated, int skipped, List<String> warnings) {
    public MemoryUpdateReport {
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
    }
}
