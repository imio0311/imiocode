package io.imiocode.mcp.config;

import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpEnvironmentResolverTest {
    @TempDir
    Path tempDirectory;

    @Test
    void expandsMultipleVariablesAndRegistersSecrets() {
        SecretRedactor redactor = new SecretRedactor("");
        McpServerConfig config = config(
                Map.of("TOKEN", "${FIRST}-${SECOND}"),
                Map.of("Authorization", "Bearer ${FIRST}"));

        McpEnvironmentResolver.Resolution result = new McpEnvironmentResolver().resolve(
                config,
                Map.of("FIRST", "alpha-secret", "SECOND", "beta-secret"),
                redactor);

        assertTrue(result.success());
        assertEquals("alpha-secret-beta-secret", result.config().env().get("TOKEN"));
        assertEquals("Bearer alpha-secret", result.config().headers().get("Authorization"));
        assertEquals("*** and ***", redactor.redact("alpha-secret and beta-secret"));
        assertFalse(result.config().toString().contains("alpha-secret"));
    }

    @Test
    void reportsOnlyMissingVariableName() {
        McpEnvironmentResolver.Resolution result = new McpEnvironmentResolver().resolve(
                config(Map.of("TOKEN", "prefix-${MISSING_TOKEN}-suffix"), Map.of()),
                Map.of(),
                new SecretRedactor(""));

        assertFalse(result.success());
        assertEquals("MISSING_TOKEN", result.missingVariable());
    }

    private McpServerConfig config(Map<String, String> env, Map<String, String> headers) {
        return new McpServerConfig(
                "test",
                true,
                McpTransportType.STDIO,
                "java",
                java.util.List.of(),
                null,
                env,
                headers,
                null,
                null,
                tempDirectory.resolve("mcp.yaml"));
    }
}
