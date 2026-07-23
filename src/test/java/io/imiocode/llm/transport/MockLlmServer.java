package io.imiocode.llm.transport;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class MockLlmServer implements AutoCloseable {
    private final HttpServer server;
    private final BlockingQueue<ResponseSpec> responses = new LinkedBlockingQueue<>();
    private final BlockingQueue<RecordedRequest> requests = new LinkedBlockingQueue<>();

    public MockLlmServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public URI baseUri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    public void enqueueSse(String body) {
        enqueue(200, "text/event-stream", List.of(body), 0);
    }

    public void enqueue(int status, String contentType, List<String> chunks, long delayMillis) {
        responses.add(new ResponseSpec(status, contentType, List.copyOf(chunks), delayMillis));
    }

    public RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = requests.poll(3, TimeUnit.SECONDS);
        if (request == null) {
            throw new AssertionError("未收到请求");
        }
        return request;
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        requests.add(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI(),
                copyHeaders(exchange.getRequestHeaders()),
                new String(requestBody, StandardCharsets.UTF_8)));

        ResponseSpec response = responses.poll();
        if (response == null) {
            response = new ResponseSpec(500, "application/json", List.of("{}"), 0);
        }
        exchange.getResponseHeaders().set("Content-Type", response.contentType());
        exchange.sendResponseHeaders(response.status(), 0);
        try (OutputStream output = exchange.getResponseBody()) {
            for (String chunk : response.chunks()) {
                output.write(chunk.getBytes(StandardCharsets.UTF_8));
                output.flush();
                if (response.delayMillis() > 0) {
                    try {
                        Thread.sleep(response.delayMillis());
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private static Map<String, List<String>> copyHeaders(Headers headers) {
        Map<String, List<String>> copy = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.forEach((name, values) -> copy.put(name, new ArrayList<>(values)));
        return copy;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private record ResponseSpec(int status, String contentType, List<String> chunks, long delayMillis) {
    }

    public record RecordedRequest(String method, URI uri, Map<String, List<String>> headers, String body) {
        public String firstHeader(String name) {
            List<String> values = headers.get(name);
            return values == null || values.isEmpty() ? null : values.get(0);
        }
    }
}
