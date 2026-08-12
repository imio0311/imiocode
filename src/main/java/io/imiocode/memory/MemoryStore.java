package io.imiocode.memory;

/** 按用户或项目作用域读取并原子替换记忆文档。 */
public interface MemoryStore {
    MemoryDocument load(MemoryScope scope);
    void replace(MemoryDocument document);
}
