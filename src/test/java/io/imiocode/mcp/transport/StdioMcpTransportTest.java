package io.imiocode.mcp.transport;

import com.fasterxml.jackson.databind.node.IntNode;
import io.imiocode.mcp.config.McpTransportType;
import io.imiocode.mcp.config.ResolvedMcpServerConfig;
import io.imiocode.mcp.fixture.FakeStdioMcpServer;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.jsonrpc.JsonRpcRequest;
import io.imiocode.mcp.jsonrpc.JsonRpcResponse;
import io.imiocode.mcp.jsonrpc.JsonRpcNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StdioMcpTransportTest {
    @TempDir
    Path tempDirectory;

    @Test
    void sendsUtf8MessagesAndIsolatesEnvironment() throws Exception {
        StdioMcpTransport transport = transport("environment", Map.of("EXPLICIT_TEST", "中文值"));
        LinkedBlockingQueue<JsonRpcResponse> responses = new LinkedBlockingQueue<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        transport.start(message -> responses.add((JsonRpcResponse) message), failure::set).join();

        transport.send(new JsonRpcRequest(
                new io.imiocode.mcp.jsonrpc.JsonRpcId(IntNode.valueOf(1)),
                "environment",
                null)).join();
        JsonRpcResponse response = responses.poll(5, TimeUnit.SECONDS);

        assertNotNull(response);
        assertEquals("中文值", response.result().path("explicit").asText());
        List<String> names = new java.util.ArrayList<>();
        response.result().path("names").forEach(node -> names.add(node.asText()));
        assertTrue(names.stream().anyMatch(name -> "PATH".equalsIgnoreCase(name)));
        assertTrue(names.contains("EXPLICIT_TEST"));
        assertFalse(names.contains("DEEPSEEK_API_KEY"));
        assertEquals(null, failure.get());
        transport.close();
        assertFalse(transport.isOpen());
    }

    @Test
    void drainsBoundedStderrAndReportsMalformedStdout() throws Exception {
        StdioMcpTransport noisy = transport("stderr", Map.of());
        LinkedBlockingQueue<JsonRpcResponse> responses = new LinkedBlockingQueue<>();
        noisy.start(message -> responses.add((JsonRpcResponse) message), ignored -> { }).join();
        noisy.send(request(1, "ping")).join();
        assertNotNull(responses.poll(5, TimeUnit.SECONDS));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (noisy.stderrTail().length() < StdioMcpTransport.MAX_STDERR_CHARS
                && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(noisy.stderrTail().length() <= StdioMcpTransport.MAX_STDERR_CHARS);
        noisy.close();

        StdioMcpTransport malformed = transport("malformed", Map.of());
        AtomicReference<Throwable> failure = new AtomicReference<>();
        malformed.start(ignored -> { }, failure::set).join();
        malformed.send(request(2, "ping")).join();
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (failure.get() == null && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertNotNull(failure.get());
        malformed.close();
    }

    @Test
    void fakeServerSupportsMcpHandshakeAndToolDiscovery() throws Exception {
        StdioMcpTransport transport = transport("normal", Map.of());
        LinkedBlockingQueue<JsonRpcResponse> responses = new LinkedBlockingQueue<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        transport.start(message -> responses.add((JsonRpcResponse) message), failure::set).join();
        transport.send(request(1, "initialize")).join();
        JsonRpcResponse initialize = responses.poll(5, TimeUnit.SECONDS);
        assertNotNull(initialize, transport.stderrTail());
        assertEquals("2025-11-25", initialize.result().path("protocolVersion").asText());
        transport.send(new JsonRpcNotification("notifications/initialized", null)).join();
        transport.send(request(2, "tools/list")).join();
        JsonRpcResponse tools = responses.poll(5, TimeUnit.SECONDS);
        assertNotNull(tools, transport.stderrTail());
        assertEquals(1, tools.result().path("tools").size());
        assertEquals(null, failure.get());
        transport.close();
    }

    private JsonRpcRequest request(int id, String method) {
        return new JsonRpcRequest(
                new io.imiocode.mcp.jsonrpc.JsonRpcId(IntNode.valueOf(id)),
                method,
                null);
    }

    private StdioMcpTransport transport(String mode, Map<String, String> env) {
        String javaExecutable = Path.of(
                System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
        ResolvedMcpServerConfig config = new ResolvedMcpServerConfig(
                "fake",
                McpTransportType.STDIO,
                javaExecutable,
                List.of("-cp", System.getProperty("java.class.path"),
                        FakeStdioMcpServer.class.getName(), mode),
                null,
                env,
                Map.of(),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                tempDirectory.resolve(".imiocode/mcp.yaml"));
        return new StdioMcpTransport(config, new JsonRpcCodec());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
