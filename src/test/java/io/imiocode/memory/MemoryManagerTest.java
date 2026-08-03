package io.imiocode.memory;

import io.imiocode.config.MemoryConfig;
import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemoryManagerTest {
    @TempDir Path root;

    @Test
    void supportsCrudDedupAndCandidateUpdate() {
        MemoryConfig config = MemoryConfig.defaults();
        MemoryManager manager = manager(config);
        MemoryEntry added = manager.add(MemoryScope.USER, "回答保持简洁");
        MemoryEntry edited = manager.edit(MemoryScope.USER, added.id(), "回答保持非常简洁");
        assertEquals(added.id(), edited.id());
        assertThrows(MemoryException.class, () -> manager.add(MemoryScope.USER, "回答保持非常简洁"));

        MemoryUpdateReport report = manager.applyCandidates(List.of(new MemoryCandidate(
                MemoryScope.USER, MemoryCategory.PREFERENCE, "默认使用中文", Optional.empty())));
        assertEquals(1, report.added());
        manager.forget(MemoryScope.USER, added.id());
        assertEquals(1, manager.loadEnabledScopes().getFirst().entries().size());
    }

    @Test
    void rejectsSecretsAndTemporaryAutoMemory() {
        MemoryManager manager = manager(MemoryConfig.defaults());
        assertThrows(MemoryException.class,
                () -> manager.add(MemoryScope.USER, "api_key: known-secret"));
        MemoryUpdateReport report = manager.applyCandidates(List.of(new MemoryCandidate(
                MemoryScope.PROJECT, MemoryCategory.PROJECT_FACT, "当前任务临时改一下", Optional.empty())));
        assertEquals(1, report.skipped());
    }

    private MemoryManager manager(MemoryConfig config) {
        var redactor = new SecretRedactor("known-secret");
        return new MemoryManager(new MarkdownMemoryStore(root.resolve("home"), root.resolve("project")),
                new MemorySafetyPolicy(config, redactor), config);
    }
}
