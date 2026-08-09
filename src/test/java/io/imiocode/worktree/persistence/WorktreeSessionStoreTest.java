package io.imiocode.worktree.persistence;

import io.imiocode.worktree.WorktreeException;
import io.imiocode.worktree.model.WorktreeSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class WorktreeSessionStoreTest {
    @TempDir Path temp;

    @Test void atomicallyRoundTripsAndClearsSession() {
        WorktreeSessionStore store = new WorktreeSessionStore(temp);
        WorktreeSession expected = new WorktreeSession("session-1", "demo", temp,
                temp.resolve(".imiocode/worktrees/demo"), "worktree-demo", "main", "abc123",
                Instant.parse("2026-08-09T00:00:00Z"));
        store.save(expected);
        assertEquals(expected, store.load().orElseThrow());
        assertTrue(store.exists());
        store.clear();
        assertTrue(store.load().isEmpty());
    }

    @Test void damagedRecordIsRejectedWithoutDeletion() throws Exception {
        WorktreeSessionStore store = new WorktreeSessionStore(temp);
        Files.createDirectories(store.file().getParent());
        Files.writeString(store.file(), "{broken");
        assertThrows(WorktreeException.class, store::load);
        assertTrue(Files.exists(store.file()));
    }
}
