package io.imiocode.memory;

import java.util.Optional;

public record MemoryCandidate(MemoryScope scope, MemoryCategory category,
                              String content, Optional<String> replacesId) {
    public MemoryCandidate {
        if (scope == null || category == null || content == null || content.isBlank()) {
            throw new IllegalArgumentException("记忆候选不完整");
        }
        content = MemoryEntry.normalizeLine(content);
        replacesId = replacesId == null ? Optional.empty() : replacesId.filter(value -> !value.isBlank());
    }
}
