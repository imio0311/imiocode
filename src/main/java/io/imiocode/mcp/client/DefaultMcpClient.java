package io.imiocode.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.mcp.jsonrpc.JsonRpcError;
import io.imiocode.mcp.jsonrpc.JsonRpcId;
import io.imiocode.mcp.jsonrpc.JsonRpcMessage;
import io.imiocode.mcp.jsonrpc.JsonRpcNotification;
import io.imiocode.mcp.jsonrpc.JsonRpcRequest;
import io.imiocode.mcp.jsonrpc.JsonRpcResponse;
import io.imiocode.mcp.transport.McpTransport;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** 自研的 MCP 2025-11-25 Tools Client。 */
public final class DefaultMcpClient implements McpClient {
    public static final String PROTOCOL_VERSION = "2025-11-25";
    private static final int MAX_TOOL_PAGES = 100;

    private final McpTransport transport;
    private final ObjectMapper mapper;
    private final String clientName;
    private final String clientVersion;
    private final Duration initializationTimeout;
    private final Duration defaultCallTimeout;
    private final AtomicLong nextRequestId = new AtomicLong();
    private final ConcurrentHashMap<JsonRpcId, CompletableFuture<JsonNode>> pendingRequests =
            new ConcurrentHashMap<>();
    private final AtomicReference<McpClientState> state =
            new AtomicReference<>(McpClientState.NEW);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<List<McpRemoteTool>> cachedTools = new AtomicReference<>();

    public DefaultMcpClient(
            McpTransport transport,
            String clientName,
            String clientVersion,
            Duration initializationTimeout,
            Duration defaultCallTimeout) {
        this(transport, new ObjectMapper(), clientName, clientVersion,
                initializationTimeout, defaultCallTimeout);
    }

    DefaultMcpClient(
            McpTransport transport,
            ObjectMapper mapper,
            String clientName,
            String clientVersion,
            Duration initializationTimeout,
            Duration defaultCallTimeout) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.clientName = requireText(clientName, "clientName");
        this.clientVersion = requireText(clientVersion, "clientVersion");
        this.initializationTimeout = positive(initializationTimeout, "initializationTimeout");
        this.defaultCallTimeout = positive(defaultCallTimeout, "defaultCallTimeout");
    }

    @Override
    public CompletableFuture<McpInitializeResult> connect() {
        if (!state.compareAndSet(McpClientState.NEW, McpClientState.CONNECTING)) {
            return CompletableFuture.failedFuture(new IllegalStateException("MCP Client 已启动"));
        }
        ObjectNode params = mapper.createObjectNode();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.putObject("capabilities");
        ObjectNode clientInfo = params.putObject("clientInfo");
        clientInfo.put("name", clientName);
        clientInfo.put("version", clientVersion);

        return transport.start(this::onInbound, this::onTransportFailure)
                .thenCompose(ignored -> sendRequest("initialize", params, initializationTimeout).future())
                .thenApply(this::parseInitializeResult)
                .thenCompose(result -> transport.send(new JsonRpcNotification(
                                "notifications/initialized", mapper.createObjectNode()))
                        .thenApply(ignored -> result))
                .whenComplete((result, failure) -> {
                    if (failure == null) {
                        state.set(McpClientState.READY);
                    } else {
                        failAll(unwrap(failure));
                    }
                });
    }

    @Override
    public CompletableFuture<List<McpRemoteTool>> listTools() {
        if (state.get() != McpClientState.READY) {
            return CompletableFuture.failedFuture(new IllegalStateException("MCP Client 尚未就绪"));
        }
        List<McpRemoteTool> cached = cachedTools.get();
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return listPage(null, new ArrayList<>(), new HashSet<>(), 0)
                .thenApply(tools -> {
                    List<McpRemoteTool> immutable = List.copyOf(tools);
                    cachedTools.compareAndSet(null, immutable);
                    return cachedTools.get();
                });
    }

    private CompletableFuture<List<McpRemoteTool>> listPage(
            String cursor,
            List<McpRemoteTool> collected,
            Set<String> seenCursors,
            int page) {
        if (page >= MAX_TOOL_PAGES) {
            return CompletableFuture.failedFuture(new IllegalStateException("MCP 工具分页超过限制"));
        }
        ObjectNode params = mapper.createObjectNode();
        if (cursor != null) {
            params.put("cursor", cursor);
        }
        return sendRequest("tools/list", params, initializationTimeout).future()
                .thenCompose(result -> {
                    JsonNode tools = result.path("tools");
                    if (!tools.isArray()) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("tools/list 缺少 tools 数组"));
                    }
                    for (JsonNode tool : tools) {
                        JsonNode schema = tool.get("inputSchema");
                        if (schema != null && !schema.isObject()) {
                            return CompletableFuture.failedFuture(
                                    new IllegalStateException("远程工具 inputSchema 必须是对象"));
                        }
                        collected.add(new McpRemoteTool(
                                tool.path("name").asText(""),
                                tool.path("description").asText(""),
                                schema == null ? null : (ObjectNode) schema));
                    }
                    JsonNode nextCursor = result.get("nextCursor");
                    if (nextCursor == null || nextCursor.isNull() || nextCursor.asText().isBlank()) {
                        return CompletableFuture.completedFuture(collected);
                    }
                    if (!nextCursor.isTextual() || !seenCursors.add(nextCursor.textValue())) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("MCP 工具分页 cursor 重复或无效"));
                    }
                    return listPage(nextCursor.textValue(), collected, seenCursors, page + 1);
                });
    }

    @Override
    public McpCallHandle callTool(
            String toolName,
            ObjectNode arguments,
            Duration timeout) {
        if (state.get() != McpClientState.READY) {
            throw new IllegalStateException("MCP Client 尚未就绪");
        }
        ObjectNode params = mapper.createObjectNode();
        params.put("name", requireText(toolName, "toolName"));
        params.set("arguments", Objects.requireNonNull(arguments, "arguments").deepCopy());
        PendingRequest pending = sendRequest(
                "tools/call", params, Objects.requireNonNullElse(timeout, defaultCallTimeout));
        CompletableFuture<McpCallResult> result = pending.future().thenApply(this::parseCallResult);
        return new McpCallHandle(pending.id(), result);
    }

    @Override
    public CompletableFuture<Void> cancelRequest(JsonRpcId requestId, String reason) {
        Objects.requireNonNull(requestId, "requestId");
        CompletableFuture<JsonNode> pending = pendingRequests.remove(requestId);
        if (pending == null) {
            return CompletableFuture.completedFuture(null);
        }
        pending.completeExceptionally(new CancellationException("MCP 请求已取消"));
        if (!transport.isOpen()) {
            return CompletableFuture.completedFuture(null);
        }
        ObjectNode params = mapper.createObjectNode();
        params.set("requestId", requestId.value());
        if (reason != null && !reason.isBlank()) {
            params.put("reason", reason.trim());
        }
        return transport.send(new JsonRpcNotification("notifications/cancelled", params))
                .exceptionally(ignored -> null);
    }

    @Override
    public McpClientState state() {
        return state.get();
    }

    int pendingRequestCount() {
        return pendingRequests.size();
    }

    private PendingRequest sendRequest(String method, ObjectNode params, Duration timeout) {
        JsonRpcId id = new JsonRpcId(LongNode.valueOf(nextRequestId.incrementAndGet()));
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        future.orTimeout(positive(timeout, "timeout").toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((result, failure) -> {
                    if (failure != null && unwrap(failure) instanceof TimeoutException) {
                        cancelRequest(id, "timeout");
                    } else {
                        pendingRequests.remove(id, future);
                    }
                });
        transport.send(new JsonRpcRequest(id, method, params))
                .whenComplete((ignored, failure) -> {
                    if (failure != null && pendingRequests.remove(id, future)) {
                        future.completeExceptionally(unwrap(failure));
                    }
                });
        return new PendingRequest(id, future);
    }

    private void onInbound(JsonRpcMessage message) {
        switch (message) {
            case JsonRpcResponse response -> {
                CompletableFuture<JsonNode> future = pendingRequests.remove(response.id());
                if (future == null) {
                    return;
                }
                if (response.successful()) {
                    future.complete(response.result());
                } else {
                    future.completeExceptionally(new McpRemoteException(
                            response.error().code(), response.error().message()));
                }
            }
            case JsonRpcRequest request -> transport.send(new JsonRpcResponse(
                    request.id(),
                    null,
                    new JsonRpcError(-32601, "Method not found", mapper.createObjectNode())));
            case JsonRpcNotification ignored -> {
                // 本章不消费动态工具变更、日志等通知。
            }
        }
    }

    private McpInitializeResult parseInitializeResult(JsonNode result) {
        if (!result.isObject()) {
            throw new IllegalStateException("initialize 结果必须是对象");
        }
        String version = result.path("protocolVersion").asText("");
        if (!PROTOCOL_VERSION.equals(version)) {
            throw new IllegalStateException("MCP 协议版本不兼容");
        }
        JsonNode capabilities = result.path("capabilities");
        if (!capabilities.isObject() || !capabilities.path("tools").isObject()) {
            throw new IllegalStateException("MCP Server 未声明 tools capability");
        }
        JsonNode info = result.path("serverInfo");
        McpServerInfo serverInfo = new McpServerInfo(
                info.path("name").asText(""),
                info.path("version").asText(""));
        return new McpInitializeResult(
                version,
                serverInfo,
                capabilities,
                result.path("instructions").asText(""));
    }

    private McpCallResult parseCallResult(JsonNode result) {
        if (!result.isObject()) {
            throw new IllegalStateException("tools/call 结果必须是对象");
        }
        List<JsonNode> content = new ArrayList<>();
        JsonNode contentNode = result.path("content");
        if (contentNode.isArray()) {
            contentNode.forEach(content::add);
        }
        return new McpCallResult(
                content,
                result.get("structuredContent"),
                result.path("isError").asBoolean(false));
    }

    private void onTransportFailure(Throwable failure) {
        failAll(failure);
    }

    private void failAll(Throwable failure) {
        if (!closed.get()) {
            state.set(McpClientState.FAILED);
        }
        pendingRequests.forEach((id, future) -> future.completeExceptionally(failure));
        pendingRequests.clear();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        state.set(McpClientState.CLOSED);
        failAll(new CancellationException("MCP Client 已关闭"));
        transport.close();
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " 必须为正数");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }

    private record PendingRequest(JsonRpcId id, CompletableFuture<JsonNode> future) {
    }

    /** 只保留标准错误码和安全 message，不向上层传递远程 data。 */
    public static final class McpRemoteException extends RuntimeException {
        private final int code;

        McpRemoteException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int code() {
            return code;
        }
    }
}
