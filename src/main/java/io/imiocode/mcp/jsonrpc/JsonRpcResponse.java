package io.imiocode.mcp.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/** JSON-RPC 成功或错误响应。 */
public record JsonRpcResponse(JsonRpcId id, JsonNode result, JsonRpcError error)
        implements JsonRpcMessage {
    public JsonRpcResponse {
        id = Objects.requireNonNull(id, "id");
        if ((result == null) == (error == null)) {
            throw new IllegalArgumentException("Response 必须且只能包含 result 或 error");
        }
        result = result == null ? null : result.deepCopy();
    }

    @Override
    public JsonNode result() {
        return result == null ? null : result.deepCopy();
    }

    public boolean successful() {
        return error == null;
    }
}
