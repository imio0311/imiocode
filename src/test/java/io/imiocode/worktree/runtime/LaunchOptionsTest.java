package io.imiocode.worktree.runtime;

import io.imiocode.worktree.WorktreeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LaunchOptionsTest {
    @Test void onlyExplicitResumeIsAccepted() {
        assertFalse(LaunchOptions.parse(new String[0]).resume());
        assertTrue(LaunchOptions.parse(new String[]{"--resume"}).resume());
        assertThrows(WorktreeException.class, () -> LaunchOptions.parse(new String[]{"--unknown"}));
        assertThrows(WorktreeException.class, () -> LaunchOptions.parse(new String[]{"--resume", "extra"}));
    }
}
