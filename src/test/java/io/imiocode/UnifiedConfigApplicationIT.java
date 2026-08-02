package io.imiocode;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.config.ConfigLoader;
import io.imiocode.config.ConfigSource;
import io.imiocode.config.RuntimeConfig;
import io.imiocode.mcp.fixture.FakeStdioMcpServer;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.manager.McpManager;
import io.imiocode.mcp.manager.McpStartupResult;
import io.imiocode.mcp.transport.McpTransportFactory;
import io.imiocode.permission.PermissionAction;
import io.imiocode.permission.PermissionRequest;
import io.imiocode.permission.PermissionRequestFactory;
import io.imiocode.permission.rule.PermissionRuleEngine;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedConfigApplicationIT {
    @TempDir
    Path root;

    @Test
    void unifiedConfigLoadsRegistersRealMcpToolAndAppliesPermissionRule() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("workspace"));
        Path userHome = Files.createDirectories(root.resolve("home"));
        String javaExecutable = Path.of(
                System.getProperty("java.home"),
                "bin",
                isWindows() ? "java.exe" : "java").toString();
        String classPath = System.getProperty("java.class.path");
        String testSecret = "unified-application-test-secret";
        Files.writeString(workspace.resolve("config.yaml"), """
                provider: deepseek
                model: integration-test
                providers:
                  deepseek:
                    api-key: ${APPLICATION_TEST_KEY}
                mcp:
                  servers:
                    fake:
                      transport: stdio
                      command: '%s'
                      args:
                        - -cp
                        - '%s'
                        - '%s'
                        - normal
                      initialization-timeout-seconds: 5
                      call-timeout-seconds: 5
                permissions:
                  mode: ask
                  rules:
                    - action: allow
                      tool: mcp_fake__echo_text
                """.formatted(
                yamlSingleQuoted(javaExecutable),
                yamlSingleQuoted(classPath),
                FakeStdioMcpServer.class.getName()), StandardCharsets.UTF_8);

        RuntimeConfig config = new ConfigLoader().loadAll(
                workspace,
                userHome,
                Map.of("APPLICATION_TEST_KEY", testSecret));

        assertEquals(ConfigSource.UNIFIED, config.sources().mcp());
        assertEquals(ConfigSource.UNIFIED, config.sources().permissions());
        assertTrue(config.notices().isEmpty());
        assertFalse(config.toString().contains(testSecret));

        ToolRegistry registry = new ToolRegistry();
        McpManager manager = new McpManager(
                new McpTransportFactory(new JsonRpcCodec()),
                request -> true,
                event -> { },
                ToolLimits.defaults(),
                config.redactor(),
                "test");
        try {
            McpStartupResult startup = manager.start(config.mcp().servers().values(), registry);
            assertEquals(1, startup.connectedServers(), startup.errors().toString());
            assertEquals(1, startup.registeredTools());

            String toolName = "mcp_fake__echo_text";
            ToolCall call = new ToolCall(
                    "unified-call",
                    toolName,
                    JsonNodeFactory.instance.objectNode().put("text", "统一配置"));
            PermissionRequest request = new PermissionRequestFactory(config.redactor()).create(
                    call,
                    registry.findEnabled(toolName).orElseThrow().definition());

            assertEquals(
                    PermissionAction.ALLOW,
                    new PermissionRuleEngine()
                            .evaluate(request, config.permissions())
                            .orElseThrow()
                            .action());
        } finally {
            manager.close();
        }
    }

    private static String yamlSingleQuoted(String value) {
        return value.replace("'", "''");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("win");
    }
}
