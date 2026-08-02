package io.imiocode.config;

import io.imiocode.permission.PermissionAction;
import io.imiocode.permission.PermissionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedConfigLoaderTest {
    @TempDir
    Path root;

    @Test
    void unifiedDomainsTakeOverLegacyFilesAndShareRedactor() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("workspace"));
        Path home = Files.createDirectories(root.resolve("home"));
        write(workspace.resolve("config.yaml"), """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: ${MODEL_KEY}
                mcp:
                  servers:
                    unified:
                      transport: stdio
                      command: java
                      env:
                        TOKEN: ${MCP_TOKEN}
                permissions:
                  mode: read-only
                  rules:
                    - action: deny
                      tool: bash
                """);
        write(workspace.resolve(".imiocode/mcp.local.yaml"), """
                servers:
                  legacy:
                    transport: stdio
                    command: legacy-command
                """);
        write(workspace.resolve(".imiocode/permissions.local.yaml"), """
                mode: full-access
                rules:
                  - action: allow
                    tool: bash
                """);

        RuntimeConfig config = new ConfigLoader().loadAll(
                workspace,
                home,
                Map.of("MODEL_KEY", "model-test-secret", "MCP_TOKEN", "mcp-test-secret"));

        assertEquals(ConfigSource.UNIFIED, config.sources().app());
        assertEquals(ConfigSource.UNIFIED, config.sources().mcp());
        assertEquals(ConfigSource.UNIFIED, config.sources().permissions());
        assertEquals(1, config.mcp().servers().size());
        assertTrue(config.mcp().servers().containsKey("unified"));
        assertFalse(config.mcp().servers().containsKey("legacy"));
        assertEquals(PermissionMode.READ_ONLY, config.permissions().mode());
        assertEquals(PermissionAction.DENY, config.permissions().projectRules().getFirst().action());
        assertEquals(0, config.notices().size());
        assertEquals("*** and ***", config.redactor().redact(
                "model-test-secret and mcp-test-secret"));
        assertFalse(config.toString().contains("model-test-secret"));
        assertFalse(config.toString().contains("mcp-test-secret"));
    }

    @Test
    void absentDomainsUseLegacyFilesAndEmitOneNoticeEach() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("legacy-workspace"));
        Path home = Files.createDirectories(root.resolve("legacy-home"));
        writeBaseConfig(workspace);
        write(workspace.resolve(".imiocode/mcp.yaml"), """
                servers:
                  legacy:
                    transport: stdio
                    command: java
                """);
        write(home.resolve(".imiocode/permissions.yaml"), """
                mode: lockdown
                rules: []
                """);

        RuntimeConfig config = new ConfigLoader().loadAll(workspace, home, Map.of());

        assertEquals(ConfigSource.LEGACY, config.sources().mcp());
        assertEquals(ConfigSource.LEGACY, config.sources().permissions());
        assertTrue(config.mcp().servers().containsKey("legacy"));
        assertEquals(PermissionMode.LOCKDOWN, config.permissions().mode());
        assertEquals(2, config.notices().size());
        assertEquals("legacy_mcp_config", config.notices().getFirst().code());
        assertEquals("legacy_permission_config", config.notices().get(1).code());
    }

    @Test
    void absentDomainsWithoutLegacyFilesUseDefaultsWithoutNotices() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("default-workspace"));
        Path home = Files.createDirectories(root.resolve("default-home"));
        writeBaseConfig(workspace);

        RuntimeConfig config = new ConfigLoader().loadAll(workspace, home, Map.of());

        assertEquals(ConfigSource.DEFAULT, config.sources().mcp());
        assertEquals(ConfigSource.DEFAULT, config.sources().permissions());
        assertTrue(config.mcp().servers().isEmpty());
        assertEquals(PermissionMode.ASK, config.permissions().mode());
        assertTrue(config.notices().isEmpty());
    }

    @Test
    void invalidUnifiedPermissionsNeverFallBackToValidLegacyRules() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("invalid-workspace"));
        Path home = Files.createDirectories(root.resolve("invalid-home"));
        write(workspace.resolve("config.yaml"), """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: test-key
                permissions:
                  mode: unknown-mode
                """);
        write(workspace.resolve(".imiocode/permissions.local.yaml"), """
                mode: full-access
                rules: []
                """);

        assertThrows(ConfigException.class,
                () -> new ConfigLoader().loadAll(workspace, home, Map.of()));
    }

    @Test
    void emptyUnifiedDomainsExplicitlyDisableLegacyConfiguration() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("empty-workspace"));
        Path home = Files.createDirectories(root.resolve("empty-home"));
        write(workspace.resolve("config.yaml"), """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: test-key
                mcp: {}
                permissions: {}
                """);
        write(workspace.resolve(".imiocode/mcp.local.yaml"), """
                servers:
                  legacy:
                    transport: stdio
                    command: java
                """);
        write(workspace.resolve(".imiocode/permissions.local.yaml"), """
                mode: full-access
                rules: []
                """);

        RuntimeConfig config = new ConfigLoader().loadAll(workspace, home, Map.of());

        assertEquals(ConfigSource.UNIFIED, config.sources().mcp());
        assertEquals(ConfigSource.UNIFIED, config.sources().permissions());
        assertTrue(config.mcp().servers().isEmpty());
        assertEquals(PermissionMode.ASK, config.permissions().mode());
        assertTrue(config.notices().isEmpty());
    }

    @Test
    void repositoryExampleIsACompleteLoadableUnifiedConfiguration() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("example-workspace"));
        Path home = Files.createDirectories(root.resolve("example-home"));
        Files.copy(Path.of("config.example.yaml"), workspace.resolve("config.yaml"));

        RuntimeConfig config = new ConfigLoader().loadAll(
                workspace,
                home,
                Map.of("DEEPSEEK_API_KEY", "example-test-key"));

        assertEquals(ConfigSource.UNIFIED, config.sources().app());
        assertEquals(ConfigSource.UNIFIED, config.sources().mcp());
        assertEquals(ConfigSource.UNIFIED, config.sources().permissions());
        assertTrue(config.mcp().servers().containsKey("context7"));
        assertEquals(PermissionMode.ASK, config.permissions().mode());
        assertEquals(2, config.permissions().projectRules().size());
    }

    private static void writeBaseConfig(Path workspace) throws Exception {
        write(workspace.resolve("config.yaml"), """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: test-key
                """);
    }

    private static void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
