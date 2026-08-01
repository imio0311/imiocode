package io.imiocode.mcp.config;

import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpConfigLoaderTest {
    @TempDir
    Path tempDirectory;

    @Test
    void mergesThreeLayersWithUserPriorityAndWholeReplacement() throws Exception {
        Path workspace = Files.createDirectories(tempDirectory.resolve("工作区"));
        Path userHome = Files.createDirectories(tempDirectory.resolve("用户"));
        write(workspace.resolve(".imiocode/mcp.local.yaml"), """
                servers:
                  local:
                    transport: stdio
                    command: local-command
                  shared:
                    transport: stdio
                    command: local-shared
                    env:
                      LOW_ONLY: should-not-survive
                """);
        write(workspace.resolve(".imiocode/mcp.yaml"), """
                servers:
                  project:
                    transport: stdio
                    command: project-command
                  shared:
                    transport: stdio
                    command: project-shared
                """);
        write(userHome.resolve(".imiocode/mcp.yaml"), """
                servers:
                  user:
                    transport: streamable-http
                    url: https://example.test/mcp
                  shared:
                    transport: stdio
                    command: user-shared
                """);

        McpConfigLoadResult result = load(workspace, userHome, Map.of());

        assertEquals(4, result.servers().size());
        assertEquals("user-shared", result.servers().get("shared").command());
        assertTrue(result.servers().get("shared").env().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void isolatesInvalidServersAndMalformedLayer() throws Exception {
        Path workspace = Files.createDirectories(tempDirectory.resolve("workspace"));
        Path userHome = Files.createDirectories(tempDirectory.resolve("home"));
        write(workspace.resolve(".imiocode/mcp.yaml"), """
                servers:
                  valid:
                    transport: stdio
                    command: java
                  no-command:
                    transport: stdio
                  insecure:
                    transport: streamable-http
                    url: http://example.test/mcp
                """);
        write(userHome.resolve(".imiocode/mcp.yaml"), "servers: [");

        McpConfigLoadResult result = load(workspace, userHome, Map.of());

        assertEquals(1, result.servers().size());
        assertTrue(result.servers().containsKey("valid"));
        assertEquals(3, result.errors().size());
    }

    @Test
    void acceptsLoopbackHttpAndRejectsUrlCredentials() throws Exception {
        Path workspace = Files.createDirectories(tempDirectory.resolve("workspace"));
        write(workspace.resolve(".imiocode/mcp.yaml"), """
                servers:
                  loopback:
                    transport: streamable-http
                    url: http://127.0.0.1:8080/mcp
                  credentials:
                    transport: streamable-http
                    url: https://user:secret@example.test/mcp
                """);

        McpConfigLoadResult result = load(workspace, tempDirectory.resolve("home"), Map.of());

        assertTrue(result.servers().containsKey("loopback"));
        assertFalse(result.servers().containsKey("credentials"));
        assertEquals(1, result.errors().size());
        assertFalse(result.errors().getFirst().safeMessage().contains("secret"));
    }

    @Test
    void noFilesProducesEmptySuccess() {
        McpConfigLoadResult result = load(
                tempDirectory.resolve("workspace"),
                tempDirectory.resolve("home"),
                Map.of());

        assertTrue(result.servers().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void rejectsSymbolicLinkConfigurationWhenPlatformAllowsCreation() throws Exception {
        Path workspace = Files.createDirectories(tempDirectory.resolve("workspace-link"));
        Path real = tempDirectory.resolve("real-mcp.yaml");
        write(real, """
                servers:
                  linked:
                    transport: stdio
                    command: java
                """);
        Path link = workspace.resolve(".imiocode/mcp.yaml");
        Files.createDirectories(link.getParent());
        try {
            Files.createSymbolicLink(link, real);
        } catch (UnsupportedOperationException | java.io.IOException exception) {
            Assumptions.abort("当前平台不允许创建测试符号链接");
        }

        McpConfigLoadResult result = load(workspace, tempDirectory.resolve("home"), Map.of());

        assertTrue(result.servers().isEmpty());
        assertEquals("unsafe_file", result.errors().getFirst().code());
    }

    private static McpConfigLoadResult load(Path workspace, Path home, Map<String, String> environment) {
        return new McpConfigLoader().load(
                workspace,
                home,
                environment,
                new SecretRedactor("model-secret"));
    }

    private static void write(Path path, String value) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, value, StandardCharsets.UTF_8);
    }
}
