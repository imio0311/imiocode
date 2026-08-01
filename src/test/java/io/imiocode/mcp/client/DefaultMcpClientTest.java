package io.imiocode.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import io.imiocode.mcp.jsonrpc.JsonRpcError;
import io.imiocode.mcp.jsonrpc.JsonRpcId;
import io.imiocode.mcp.jsonrpc.JsonRpcMessage;
import io.imiocode.mcp.jsonrpc.JsonRpcNotification;
import io.imiocode.mcp.jsonrpc.JsonRpcRequest;
import io.imiocode.mcp.jsonrpc.JsonRpcResponse;
import io.imiocode.mcp.transport.McpTransport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultMcpClientTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void performsHandshakePaginationAndToolCall() throws Exception {
        ScriptedTransport transport = new ScriptedTransport(this::normalResponse);
        DefaultMcpClient client = client(transport, Duration.ofSeconds(2));

        McpInitializeResult initialized = client.connect().get(2, TimeUnit.SECONDS);
        List<McpRemoteTool> tools = client.listTools().get(2, TimeUnit.SECONDS);
        McpCallResult call = client.callTool(
                "second", mapper.createObjectNode().put("value", "中文"), Duration.ofSeconds(2))
                .result().get(2, TimeUnit.SECONDS);

        assertEquals(DefaultMcpClient.PROTOCOL_VERSION, initialized.protocolVersion());
        assertEquals("do not inject", initialized.instructions());
        assertEquals(List.of("first", "second"), tools.stream().map(McpRemoteTool::name).toList());
        assertEquals("中文", call.content().getFirst().path("text").asText());
        assertTrue(transport.sent.stream().anyMatch(message ->
                message instanceof JsonRpcNotification notification
                        && "notifications/initialized".equals(notification.method())));
        assertEquals(0, client.pendingRequestCount());
        client.close();
        assertEquals(McpClientState.CLOSED, client.state());
    }

    @Test
    void rejectsVersionMismatchAndMissingCapability() {
        ScriptedTransport wrongVersion = new ScriptedTransport(request -> initialize(
                request.id(), "2025-03-26", true));
        DefaultMcpClient first = client(wrongVersion, Duration.ofSeconds(1));
        assertThrows(Exception.class, () -> first.connect().get(2, TimeUnit.SECONDS));
        assertEquals(McpClientState.FAILED, first.state());

        ScriptedTransport noTools = new ScriptedTransport(request ->
                initialize(request.id(), DefaultMcpClient.PROTOCOL_VERSION, false));
        DefaultMcpClient second = client(noTools, Duration.ofSeconds(1));
        assertThrows(Exception.class, () -> second.connect().get(2, TimeUnit.SECONDS));
        assertEquals(McpClientState.FAILED, second.state());
    }

    @Test
    void timesOutCancelsAndClearsPendingRequest() throws Exception {
        ScriptedTransport transport = new ScriptedTransport(request ->
                "initialize".equals(request.method())
                        ? initialize(request.id(), DefaultMcpClient.PROTOCOL_VERSION, true)
                        : null);
        DefaultMcpClient client = client(transport, Duration.ofMillis(80));
        client.connect().get(2, TimeUnit.SECONDS);

        McpCallHandle handle = client.callTool(
                "slow", mapper.createObjectNode(), Duration.ofMillis(80));
        assertThrows(Exception.class, () -> handle.result().get(2, TimeUnit.SECONDS));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (client.pendingRequestCount() != 0 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertEquals(0, client.pendingRequestCount());
        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (transport.sent.stream().noneMatch(message ->
                message instanceof JsonRpcNotification notification
                        && "notifications/cancelled".equals(notification.method()))
                && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertTrue(transport.sent.stream().anyMatch(message ->
                message instanceof JsonRpcNotification notification
                        && "notifications/cancelled".equals(notification.method())));
        client.close();
    }

    @Test
    void repliesMethodNotFoundToServerRequestAndIgnoresUnknownResponse() throws Exception {
        ScriptedTransport transport = new ScriptedTransport(this::normalResponse);
        DefaultMcpClient client = client(transport, Duration.ofSeconds(1));
        client.connect().get(2, TimeUnit.SECONDS);

        transport.deliver(new JsonRpcResponse(
                new JsonRpcId(IntNode.valueOf(999)),
                mapper.createObjectNode(),
                null));
        transport.deliver(new JsonRpcRequest(
                new JsonRpcId(IntNode.valueOf(77)),
                "sampling/createMessage",
                mapper.createObjectNode()));

        JsonRpcResponse response = assertInstanceOf(
                JsonRpcResponse.class,
                transport.sent.getLast());
        assertEquals(-32601, response.error().code());
        assertEquals(McpClientState.READY, client.state());
        client.close();
    }

    private JsonRpcResponse normalResponse(JsonRpcRequest request) {
        return switch (request.method()) {
            case "initialize" -> initialize(
                    request.id(), DefaultMcpClient.PROTOCOL_VERSION, true);
            case "tools/list" -> {
                var result = mapper.createObjectNode();
                var tools = result.putArray("tools");
                if (!request.params().has("cursor")) {
                    tools.addObject()
                            .put("name", "first")
                            .put("description", "first tool")
                            .putObject("inputSchema")
                            .put("type", "object");
                    result.put("nextCursor", "page-2");
                } else {
                    tools.addObject().put("name", "second");
                }
                yield new JsonRpcResponse(request.id(), result, null);
            }
            case "tools/call" -> {
                var result = mapper.createObjectNode();
                result.putArray("content").addObject()
                        .put("type", "text")
                        .put("text", request.params().path("arguments").path("value").asText(""));
                yield new JsonRpcResponse(request.id(), result, null);
            }
            default -> new JsonRpcResponse(
                    request.id(), null, new JsonRpcError(-32601, "missing", null));
        };
    }

    private JsonRpcResponse initialize(JsonRpcId id, String version, boolean toolsCapability) {
        var result = mapper.createObjectNode();
        result.put("protocolVersion", version);
        var capabilities = result.putObject("capabilities");
        if (toolsCapability) {
            capabilities.putObject("tools");
        }
        result.putObject("serverInfo").put("name", "fake").put("version", "1.0");
        result.put("instructions", "do not inject");
        return new JsonRpcResponse(id, result, null);
    }

    private static DefaultMcpClient client(ScriptedTransport transport, Duration timeout) {
        return new DefaultMcpClient(
                transport, "ImioCode", "test", timeout, timeout);
    }

    private static final class ScriptedTransport implements McpTransport {
        private final java.util.function.Function<JsonRpcRequest, JsonRpcResponse> responder;
        private final List<JsonRpcMessage> sent = new CopyOnWriteArrayList<>();
        private volatile Consumer<JsonRpcMessage> inbound;
        private volatile boolean open;

        private ScriptedTransport(
                java.util.function.Function<JsonRpcRequest, JsonRpcResponse> responder) {
            this.responder = responder;
        }

        @Override
        public CompletableFuture<Void> start(
                Consumer<JsonRpcMessage> inboundHandler,
                Consumer<Throwable> failureHandler) {
            inbound = inboundHandler;
            open = true;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> send(JsonRpcMessage message) {
            sent.add(message);
            if (message instanceof JsonRpcRequest request) {
                JsonRpcResponse response = responder.apply(request);
                if (response != null) {
                    CompletableFuture.runAsync(() -> inbound.accept(response));
                }
            }
            return CompletableFuture.completedFuture(null);
        }

        void deliver(JsonRpcMessage message) {
            inbound.accept(message);
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }
    }
}
