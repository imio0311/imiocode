package io.imiocode.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentPlaceholderResolverTest {
    private final EnvironmentPlaceholderResolver resolver = new EnvironmentPlaceholderResolver();

    @Test
    void expandsMultiplePlaceholdersAndRegistersSecrets() {
        List<String> secrets = new ArrayList<>();

        String resolved = resolver.expand(
                "Bearer ${TOKEN}:${SUFFIX}",
                Map.of("TOKEN", "a$1\\token", "SUFFIX", "tail"),
                secrets::add);

        assertEquals("Bearer a$1\\token:tail", resolved);
        assertEquals(List.of("a$1\\token", "tail"), secrets);
    }

    @Test
    void leavesPlainTextUntouched() {
        List<String> secrets = new ArrayList<>();

        assertEquals("plain", resolver.expand("plain", Map.of(), secrets::add));
        assertEquals(List.of(), secrets);
    }

    @Test
    void reportsOnlyMissingVariableName() {
        String unrelatedSecret = "must-not-appear";

        MissingEnvironmentVariableException exception = assertThrows(
                MissingEnvironmentVariableException.class,
                () -> resolver.expand(
                        "${MISSING}",
                        Map.of("OTHER", unrelatedSecret),
                        ignored -> { }));

        assertEquals("MISSING", exception.variableName());
        assertFalse(exception.getMessage().contains(unrelatedSecret));
    }
}
