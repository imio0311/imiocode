package io.imiocode.mcp.jsonrpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/** JSON-RPC 2.0 的严格、无业务语义编解码器。 */
public final class JsonRpcCodec {
    private static final String VERSION = "2.0";
    private final ObjectMapper mapper;

    public JsonRpcCodec() {
        this(new ObjectMapper());
    }

    public JsonRpcCodec(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public String encode(JsonRpcMessage message) {
        Objects.requireNonNull(message, "message");
        ObjectNode root = mapper.createObjectNode();
        root.put("jsonrpc", VERSION);
        switch (message) {
            case JsonRpcRequest request -> {
                root.set("id", request.id().value());
                root.put("method", request.method());
                if (!request.params().isNull()) {
                    root.set("params", request.params());
                }
            }
            case JsonRpcNotification notification -> {
                root.put("method", notification.method());
                if (!notification.params().isNull()) {
                    root.set("params", notification.params());
                }
            }
            case JsonRpcResponse response -> {
                root.set("id", response.id().value());
                if (response.successful()) {
                    root.set("result", response.result());
                } else {
                    ObjectNode error = root.putObject("error");
                    error.put("code", response.error().code());
                    error.put("message", response.error().message());
                    if (!response.error().data().isNull()) {
                        error.set("data", response.error().data());
                    }
                }
            }
        }
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("无法编码 JSON-RPC 消息", exception);
        }
    }

    public JsonRpcMessage decode(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON-RPC 消息不能为空");
        }
        final JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("JSON-RPC JSON 格式错误", exception);
        }
        if (root == null || !root.isObject() || !VERSION.equals(root.path("jsonrpc").asText())) {
            throw new IllegalArgumentException("JSON-RPC 版本或消息格式无效");
        }
        JsonNode method = root.get("method");
        JsonNode id = root.get("id");
        if (method != null) {
            if (!method.isTextual() || method.textValue().isBlank()) {
                throw new IllegalArgumentException("JSON-RPC method 无效");
            }
            return id == null
                    ? new JsonRpcNotification(method.textValue(), root.get("params"))
                    : new JsonRpcRequest(new JsonRpcId(id), method.textValue(), root.get("params"));
        }
        if (id == null) {
            throw new IllegalArgumentException("JSON-RPC 响应缺少 id");
        }
        boolean hasResult = root.has("result");
        boolean hasError = root.has("error");
        if (hasResult == hasError) {
            throw new IllegalArgumentException("JSON-RPC 响应必须且只能包含 result 或 error");
        }
        JsonRpcId responseId = new JsonRpcId(id);
        if (hasResult) {
            return new JsonRpcResponse(responseId, root.get("result"), null);
        }
        JsonNode error = root.get("error");
        if (!error.isObject() || !error.path("code").isIntegralNumber()
                || !error.path("message").isTextual()) {
            throw new IllegalArgumentException("JSON-RPC error 格式无效");
        }
        return new JsonRpcResponse(
                responseId,
                null,
                new JsonRpcError(
                        error.path("code").intValue(),
                        error.path("message").textValue(),
                        error.get("data")));
    }
}
