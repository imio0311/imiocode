package io.imiocode.worktree.security;

import io.imiocode.worktree.WorktreeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class WorktreeSlugValidatorTest {
    private final WorktreeSlugValidator validator = new WorktreeSlugValidator();

    @Test void canonicalizesSafeAsciiSlug() {
        assertEquals("team_alice-1.2", validator.validate("Team_Alice-1.2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "../x", "a/b", "a\\b", ".hidden", "tail.", "a..b",
            "C:evil", "中文", "has space", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void rejectsUntrustedNames(String input) {
        assertThrows(WorktreeException.class, () -> validator.validate(input));
    }

    @Test void agentSlugsAreSafeAndUnique() {
        String first = validator.uniqueAgentSlug("general-purpose");
        String second = validator.uniqueAgentSlug("general-purpose");
        assertTrue(first.startsWith("agent-general-purpose-"));
        assertNotEquals(first, second);
        assertEquals(first, validator.validate(first));
    }
}
