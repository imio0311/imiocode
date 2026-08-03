package io.imiocode.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownMemoryStoreTest {
    @TempDir Path root;

    @Test
    void atomicallyRoundTripsBothScopes() {
        Path home = root.resolve("home"), project = root.resolve("project");
        MarkdownMemoryStore store = new MarkdownMemoryStore(home, project);
        MemoryEntry user = new MemoryEntry("m_001122334455", MemoryCategory.PREFERENCE, "使用中文");
        MemoryEntry fact = new MemoryEntry("m_abcdef123456", MemoryCategory.PROJECT_FACT, "项目使用 Java 21");

        store.replace(new MemoryDocument(MemoryScope.USER, List.of(user)));
        store.replace(new MemoryDocument(MemoryScope.PROJECT, List.of(fact)));

        assertEquals(List.of(user), store.load(MemoryScope.USER).entries());
        assertEquals(List.of(fact), store.load(MemoryScope.PROJECT).entries());
        assertTrue(Files.exists(project.resolve(".imiocode/memories.md")));
    }

    @Test
    void rejectsMalformedDocument() throws Exception {
        Path project = root.resolve("project");
        Files.createDirectories(project.resolve(".imiocode"));
        Files.writeString(project.resolve(".imiocode/memories.md"), "not a memory");
        assertThrows(MemoryException.class,
                () -> new MarkdownMemoryStore(root.resolve("home"), project).load(MemoryScope.PROJECT));
    }
}
