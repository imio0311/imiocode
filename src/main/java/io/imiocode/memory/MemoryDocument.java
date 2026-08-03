package io.imiocode.memory;

import java.util.List;
import java.util.Objects;

public record MemoryDocument(MemoryScope scope, List<MemoryEntry> entries) {
    public MemoryDocument {
        Objects.requireNonNull(scope, "scope");
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static MemoryDocument empty(MemoryScope scope) { return new MemoryDocument(scope, List.of()); }
}
