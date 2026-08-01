package io.imiocode.mcp.transport;

import io.imiocode.mcp.config.ResolvedMcpServerConfig;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.jsonrpc.JsonRpcMessage;
import io.imiocode.mcp.jsonrpc.JsonRpcRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** MCP Streamable HTTP 的 POST 子集，不建立长期 GET。 */
public final class StreamableHttpMcpTransport implements McpTransport {
    static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_REDIRECTS = 3;
    private static final Set<String> FORBIDDEN_HEADERS = Set.of(
            "host", "content-length", "connection", "upgrade", "mcp-session-id",
            "mcp-protocol-version", "content-type", "accept");

    private final ResolvedMcpServerConfig config;
    private final JsonRpcCodec codec;
    private final SseMessageDecoder sseDecoder;
    private final HttpClient client;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean failed = new AtomicBoolean();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final Set<CompletableFuture<?>> inFlight = ConcurrentHashMap.newKeySet();

    private volatile Consumer<JsonRpcMessage> inboundHandler;
    private volatile Consumer<Throwable> failureHandler;

    public StreamableHttpMcpTransport(
            ResolvedMcpServerConfig config,
            JsonRpcCodec codec) {
        this(config, codec, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(config.initializationTimeout())
                .build());
    }

    StreamableHttpMcpTransport(
            ResolvedMcpServerConfig config,
            JsonRpcCodec codec,
            HttpClient client) {
        this.config = Objects.requireNonNull(config, "config");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.sseDecoder = new SseMessageDecoder(codec);
        this.client = Objects.requireNonNull(client, "client");
        validateUri(config.url());
        validateHeaders();
    }

    @Override
    public CompletableFuture<Void> start(
            Consumer<JsonRpcMessage> inboundHandler,
            Consumer<Throwable> failureHandler) {
        if (!started.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new McpTransportException("HTTP Transport 已启动"));
        }
        this.inboundHandler = Objects.requireNonNull(inboundHandler, "inboundHandler");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> send(JsonRpcMessage message) {
        Objects.requireNonNull(message, "message");
        if (!isOpen()) {
            return CompletableFuture.failedFuture(new McpTransportException("MCP HTTP 连接未打开"));
        }
        String body = codec.encode(message);
        CompletableFuture<Void> result = sendTo(config.url(), message, body, 0);
        inFlight.add(result);
        result.whenComplete((ignored, failure) -> inFlight.remove(result));
        return result;
    }

    private CompletableFuture<Void> sendTo(
            URI uri,
            JsonRpcMessage message,
            String body,
            int redirects) {
        HttpRequest request = buildPost(uri, message, body);
        CompletableFuture<HttpResponse<InputStream>> responseFuture =
                client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
        inFlight.add(responseFuture);
        return responseFuture.thenCompose(response -> {
            inFlight.remove(responseFuture);
            final byte[] bytes;
            try {
                bytes = readBounded(response.body());
            } catch (IOException | McpTransportException exception) {
                return CompletableFuture.failedFuture(exception);
            }
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                if (redirects >= MAX_REDIRECTS) {
                    return CompletableFuture.failedFuture(
                            new McpTransportException("MCP HTTP 重定向次数超过限制"));
                }
                String location = response.headers().firstValue("Location").orElse("");
                if (location.isBlank()) {
                    return CompletableFuture.failedFuture(
                            new McpTransportException("MCP HTTP 重定向缺少 Location"));
                }
                URI target = uri.resolve(location);
                if (!sameOrigin(uri, target)) {
                    return CompletableFuture.failedFuture(
                            new McpTransportException("拒绝 MCP HTTP 跨源重定向"));
                }
                return sendTo(target, message, body, redirects + 1);
            }
            if (status == 202 && bytes.length == 0) {
                return CompletableFuture.completedFuture(null);
            }
            if (status < 200 || status >= 300) {
                McpTransportException failure = new McpTransportException(
                        "MCP HTTP 请求失败，状态码 " + status);
                if (status == 404 && sessionId.get() != null) {
                    signalFailure(failure);
                }
                return CompletableFuture.failedFuture(failure);
            }
            response.headers().firstValue("MCP-Session-Id").ifPresent(this::storeSessionId);
            String contentType = response.headers().firstValue("Content-Type")
                    .orElse("")
                    .toLowerCase(Locale.ROOT);
            try {
                if (contentType.startsWith("application/json")) {
                    if (bytes.length > 0) {
                        inboundHandler.accept(codec.decode(new String(bytes, StandardCharsets.UTF_8)));
                    }
                } else if (contentType.startsWith("text/event-stream")) {
                    sseDecoder.decode(bytes).forEach(inboundHandler);
                } else {
                    return CompletableFuture.failedFuture(
                            new McpTransportException("MCP HTTP 响应 Content-Type 无效"));
                }
                return CompletableFuture.completedFuture(null);
            } catch (RuntimeException exception) {
                return CompletableFuture.failedFuture(
                        new McpTransportException("无法解析 MCP HTTP 响应", exception));
            }
        });
    }

    private static byte[] readBounded(InputStream stream) throws IOException {
        try (stream) {
            byte[] bytes = stream.readNBytes(MAX_RESPONSE_BYTES + 1);
            if (bytes.length > MAX_RESPONSE_BYTES) {
                throw new McpTransportException("MCP HTTP 响应超过大小限制");
            }
            return bytes;
        }
    }

    private HttpRequest buildPost(URI uri, JsonRpcMessage message, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeoutFor(message))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        config.headers().forEach(builder::header);
        String currentSession = sessionId.get();
        if (currentSession != null) {
            builder.header("MCP-Session-Id", currentSession);
        }
        if (!(message instanceof JsonRpcRequest request)
                || !"initialize".equals(request.method())) {
            builder.header("MCP-Protocol-Version", "2025-11-25");
        }
        return builder.build();
    }

    private Duration timeoutFor(JsonRpcMessage message) {
        if (message instanceof JsonRpcRequest request
                && ("initialize".equals(request.method()) || "tools/list".equals(request.method()))) {
            return config.initializationTimeout();
        }
        return config.callTimeout();
    }

    private void storeSessionId(String value) {
        if (value.isEmpty() || value.chars().anyMatch(character -> character < 0x21 || character > 0x7e)) {
            throw new McpTransportException("MCP Session ID 格式无效");
        }
        sessionId.compareAndSet(null, value);
    }

    private void validateHeaders() {
        config.headers().forEach((name, value) -> {
            if (name == null || name.isBlank() || value == null
                    || name.chars().anyMatch(character -> character <= 32 || character >= 127)
                    || value.contains("\r") || value.contains("\n")
                    || FORBIDDEN_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("MCP HTTP Header 配置无效");
            }
        });
    }

    static void validateUri(URI uri) {
        Objects.requireNonNull(uri, "url");
        if (uri.getUserInfo() != null || uri.getHost() == null) {
            throw new IllegalArgumentException("MCP HTTP URL 无效");
        }
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            return;
        }
        if ("http".equalsIgnoreCase(scheme) && isLoopback(uri.getHost())) {
            return;
        }
        throw new IllegalArgumentException("远程 MCP HTTP 必须使用 HTTPS");
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }

    private static boolean sameOrigin(URI left, URI right) {
        return Objects.equals(left.getScheme(), right.getScheme())
                && Objects.equals(left.getHost(), right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private void signalFailure(Throwable failure) {
        if (failed.compareAndSet(false, true) && failureHandler != null) {
            failureHandler.accept(failure);
        }
    }

    @Override
    public boolean isOpen() {
        return started.get() && !closed.get() && !failed.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        inFlight.forEach(future -> future.cancel(true));
        inFlight.clear();
        String currentSession = sessionId.getAndSet(null);
        if (currentSession != null) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(config.url())
                        .timeout(config.initializationTimeout())
                        .header("MCP-Session-Id", currentSession)
                        .header("MCP-Protocol-Version", "2025-11-25")
                        .DELETE();
                config.headers().forEach(builder::header);
                client.sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding());
            } catch (RuntimeException ignored) {
                // Session 删除是尽力而为，不能阻塞应用关闭。
            }
        }
    }
}
