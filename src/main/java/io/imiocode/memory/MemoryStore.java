package io.imiocode.memory;

public interface MemoryStore {
    MemoryDocument load(MemoryScope scope);
    void replace(MemoryDocument document);
}
