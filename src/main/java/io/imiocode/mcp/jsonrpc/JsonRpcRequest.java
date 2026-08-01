package io.imiocode.mcp.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.Objects;

/** JSON-RPC 请求。 */
public record JsonRpcRequest(JsonRpcId id, String method, JsonNode params)
        implements JsonRpcMessage {
    public JsonRpcRequest {
        id = Objects.requireNonNull(id, "id");
        method = requireMethod(method);
        params = params == null ? NullNode.getInstance() : params.deepCopy();
    }

    @Override
    public JsonNode params() {
        return params.deepCopy();
    }

    static String requireMethod(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("method 不能为空");
        }
        return value.trim();
    }
}
