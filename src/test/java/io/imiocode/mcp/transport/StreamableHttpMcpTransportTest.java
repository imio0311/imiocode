package io.imiocode.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.imiocode.mcp.config.McpTransportType;
import io.imiocode.mcp.config.ResolvedMcpServerConfig;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.jsonrpc.JsonRpcRequest;
import io.imiocode.mcp.jsonrpc.JsonRpcResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamableHttpMcpTransportTest {
    private HttpServer server;

    @TempDir
    Path tempDirectory;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void handlesJsonSessionHeadersAndDelete() throws Exception {
        JsonRpcCodec codec = new JsonRpcCodec();
        List<Map<String, List<String>>> requestHeaders = new CopyOnWriteArrayList<>();
        CountDownLatch deleted = new CountDownLatch(1);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/mcp", exchange -> {
            requestHeaders.add(exchange.getRequestHeaders());
            if ("DELETE".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                deleted.countDown();
                return;
            }
            JsonRpcRequest request = (JsonRpcRequest) codec.decode(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = codec.encode(new JsonRpcResponse(
                    request.id(),
                    new ObjectMapper().createObjectNode().put("method", request.method()),
                    null)).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            if ("initialize".equals(request.method())) {
                exchange.getResponseHeaders().set("MCP-Session-Id", "session-123");
            }
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        StreamableHttpMcpTransport transport = transport(uri("/mcp"));
        LinkedBlockingQueue<JsonRpcResponse> responses = new LinkedBlockingQueue<>();
        transport.start(message -> responses.add((JsonRpcResponse) message), ignored -> { }).join();
        transport.send(request(1, "initialize")).join();
        assertNotNull(responses.poll(2, TimeUnit.SECONDS));
        transport.send(request(2, "tools/list")).join();
        assertNotNull(responses.poll(2, TimeUnit.SECONDS));
        transport.close();

        assertTrue(firstHeader(requestHeaders.getFirst(), "Accept").contains("text/event-stream"));
        assertEquals("session-123", firstHeader(requestHeaders.get(1), "MCP-Session-Id"));
        assertEquals("2025-11-25", firstHeader(requestHeaders.get(1), "MCP-Protocol-Version"));
        assertTrue(deleted.await(2, TimeUnit.SECONDS));
    }

    @Test
    void handlesFiniteSseResponse() throws Exception {
        JsonRpcCodec codec = new JsonRpcCodec();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sse", exchange -> {
            JsonRpcRequest request = (JsonRpcRequest) codec.decode(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = "data: " + codec.encode(new JsonRpcResponse(
                    request.id(), new ObjectMapper().createObjectNode().put("ok", true), null)) + "\n\n";
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        StreamableHttpMcpTransport transport = transport(uri("/sse"));
        LinkedBlockingQueue<JsonRpcResponse> responses = new LinkedBlockingQueue<>();
        transport.start(message -> responses.add((JsonRpcResponse) message), ignored -> { }).join();
        transport.send(request(1, "initialize")).join();

        assertTrue(responses.poll(2, TimeUnit.SECONDS).result().path("ok").asBoolean());
        transport.close();
    }

    @Test
    void validatesUrlSecurity() {
        assertThrows(IllegalArgumentException.class, () ->
                StreamableHttpMcpTransport.validateUri(URI.create("http://example.com/mcp")));
        assertThrows(IllegalArgumentException.class, () ->
                StreamableHttpMcpTransport.validateUri(URI.create("https://user:secret@example.com/mcp")));
        StreamableHttpMcpTransport.validateUri(URI.create("http://localhost:8080/mcp"));
        StreamableHttpMcpTransport.validateUri(URI.create("http://127.0.0.1:8080/mcp"));
        StreamableHttpMcpTransport.validateUri(URI.create("http://[::1]:8080/mcp"));
    }

    @Test
    void followsSameOriginButRejectsCrossOriginRedirectAndOversizedBody() throws Exception {
        JsonRpcCodec codec = new JsonRpcCodec();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/same", exchange -> redirect(exchange, "/json"));
        server.createContext("/cross", exchange -> redirect(
                exchange,
                "http://localhost:" + server.getAddress().getPort() + "/json"));
        server.createContext("/json", exchange -> {
            JsonRpcRequest request = (JsonRpcRequest) codec.decode(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = codec.encode(new JsonRpcResponse(
                    request.id(),
                    new ObjectMapper().createObjectNode().put("ok", true),
                    null)).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/large", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] response = new byte[StreamableHttpMcpTransport.MAX_RESPONSE_BYTES + 1];
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        StreamableHttpMcpTransport same = transport(uri("/same"));
        LinkedBlockingQueue<JsonRpcResponse> responses = new LinkedBlockingQueue<>();
        same.start(message -> responses.add((JsonRpcResponse) message), ignored -> { }).join();
        same.send(request(1, "initialize")).join();
        assertTrue(responses.poll(2, TimeUnit.SECONDS).result().path("ok").asBoolean());
        same.close();

        StreamableHttpMcpTransport cross = transport(uri("/cross"));
        cross.start(ignored -> { }, ignored -> { }).join();
        assertThrows(java.util.concurrent.CompletionException.class, () ->
                cross.send(request(2, "initialize")).join());
        cross.close();

        StreamableHttpMcpTransport large = transport(uri("/large"));
        large.start(ignored -> { }, ignored -> { }).join();
        assertThrows(java.util.concurrent.CompletionException.class, () ->
                large.send(request(3, "initialize")).join());
        large.close();
    }

    private StreamableHttpMcpTransport transport(URI uri) {
        return new StreamableHttpMcpTransport(new ResolvedMcpServerConfig(
                "http",
                McpTransportType.STREAMABLE_HTTP,
                "",
                List.of(),
                uri,
                Map.of(),
                Map.of("Authorization", "Bearer test"),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                tempDirectory.resolve(".imiocode/mcp.yaml")), new JsonRpcCodec());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static JsonRpcRequest request(int id, String method) {
        return new JsonRpcRequest(
                new io.imiocode.mcp.jsonrpc.JsonRpcId(IntNode.valueOf(id)),
                method,
                null);
    }

    private static String firstHeader(Map<String, List<String>> headers, String name) {
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse("");
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}
