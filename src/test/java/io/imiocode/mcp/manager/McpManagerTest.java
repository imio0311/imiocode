package io.imiocode.mcp.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.mcp.client.McpCallHandle;
import io.imiocode.mcp.client.McpCallResult;
import io.imiocode.mcp.client.McpClient;
import io.imiocode.mcp.client.McpClientState;
import io.imiocode.mcp.client.McpInitializeResult;
import io.imiocode.mcp.client.McpRemoteTool;
import io.imiocode.mcp.client.McpServerInfo;
import io.imiocode.mcp.config.McpTransportType;
import io.imiocode.mcp.config.ResolvedMcpServerConfig;
import io.imiocode.mcp.jsonrpc.JsonRpcId;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpManagerTest {
    @TempDir
    Path tempDirectory;

    @Test
    void rejectsStdioBeforeCreatingClient() {
        AtomicInteger created = new AtomicInteger();
        McpManager manager = manager(config -> {
            created.incrementAndGet();
            return new FakeClient(List.of(), false);
        }, request -> false, event -> { });

        McpStartupResult result = manager.start(
                List.of(stdio("denied")),
                new ToolRegistry());

        assertEquals(0, created.get());
        assertEquals(0, result.connectedServers());
        manager.close();
    }

    @Test
    void isolatesFailedServerRegistersDeterministicallyAndCloses() {
        List<McpEvent> events = new CopyOnWriteArrayList<>();
        Map<String, FakeClient> clients = new java.util.concurrent.ConcurrentHashMap<>();
        McpManager manager = manager(config -> {
            FakeClient client = new FakeClient(
                    config.name().equals("bad")
                            ? List.of()
                            : List.of(tool("z/tool"), tool("a tool")),
                    config.name().equals("bad"));
            clients.put(config.name(), client);
            return client;
        }, request -> true, events::add);
        ToolRegistry registry = new ToolRegistry();

        McpStartupResult result = manager.start(
                List.of(http("good"), http("bad")),
                registry);

        assertEquals(1, result.connectedServers());
        assertEquals(2, result.registeredTools());
        assertTrue(registry.enabledNames().contains("mcp_good__z_tool"));
        assertTrue(registry.enabledNames().contains("mcp_good__a_tool"));
        assertEquals(1, result.errors().size());
        assertTrue(events.stream().anyMatch(event -> event.type() == McpEventType.SERVER_FAILED));
        manager.close();
        assertEquals(McpClientState.CLOSED, clients.get("good").state());
    }

    @Test
    void skipsSanitizedCollisionAndStartsOnlyOnce() {
        McpManager manager = manager(
                config -> new FakeClient(List.of(tool("a/b"), tool("a b")), false),
                request -> true,
                event -> { });

        McpStartupResult result = manager.start(List.of(http("same")), new ToolRegistry());

        assertEquals(1, result.registeredTools());
        assertEquals(1, result.errors().size());
        assertThrows(IllegalStateException.class, () ->
                manager.start(List.of(http("same")), new ToolRegistry()));
        manager.close();
        manager.close();
    }

    private McpManager manager(
            McpManager.ClientFactory factory,
            McpLaunchApprover approver,
            McpEventListener listener) {
        return new McpManager(
                factory,
                approver,
                listener,
                ToolLimits.defaults(),
                new SecretRedactor("secret"));
    }

    private ResolvedMcpServerConfig stdio(String name) {
        return new ResolvedMcpServerConfig(
                name, McpTransportType.STDIO, "java", List.of("-version"), null,
                Map.of(), Map.of(), Duration.ofSeconds(1), Duration.ofSeconds(1),
                tempDirectory.resolve(".imiocode/mcp.yaml"));
    }

    private ResolvedMcpServerConfig http(String name) {
        return new ResolvedMcpServerConfig(
                name, McpTransportType.STREAMABLE_HTTP, "", List.of(),
                URI.create("https://example.test/mcp"),
                Map.of(), Map.of(), Duration.ofSeconds(1), Duration.ofSeconds(1),
                tempDirectory.resolve(".imiocode/mcp.yaml"));
    }

    private static McpRemoteTool tool(String name) {
        return new McpRemoteTool(
                name,
                "remote",
                new ObjectMapper().createObjectNode().put("type", "object"));
    }

    private static final class FakeClient implements McpClient {
        private final List<McpRemoteTool> tools;
        private final boolean fail;
        private volatile McpClientState state = McpClientState.NEW;

        private FakeClient(List<McpRemoteTool> tools, boolean fail) {
            this.tools = tools;
            this.fail = fail;
        }

        @Override
        public CompletableFuture<McpInitializeResult> connect() {
            if (fail) {
                state = McpClientState.FAILED;
                return CompletableFuture.failedFuture(new IllegalStateException("safe failure"));
            }
            state = McpClientState.READY;
            return CompletableFuture.completedFuture(new McpInitializeResult(
                    "2025-11-25",
                    new McpServerInfo("fake", "1"),
                    new ObjectMapper().createObjectNode().putObject("tools"),
                    ""));
        }

        @Override
        public CompletableFuture<List<McpRemoteTool>> listTools() {
            return CompletableFuture.completedFuture(tools);
        }

        @Override
        public McpCallHandle callTool(String toolName, ObjectNode arguments, Duration timeout) {
            return new McpCallHandle(
                    new JsonRpcId(IntNode.valueOf(1)),
                    CompletableFuture.completedFuture(new McpCallResult(List.of(), null, false)));
        }

        @Override
        public CompletableFuture<Void> cancelRequest(JsonRpcId requestId, String reason) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public McpClientState state() {
            return state;
        }

        @Override
        public void close() {
            state = McpClientState.CLOSED;
        }
    }
}
