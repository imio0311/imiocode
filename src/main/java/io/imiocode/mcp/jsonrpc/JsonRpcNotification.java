package io.imiocode.mcp.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

/** JSON-RPC 通知。 */
public record JsonRpcNotification(String method, JsonNode params)
        implements JsonRpcMessage {
    public JsonRpcNotification {
        method = JsonRpcRequest.requireMethod(method);
        params = params == null ? NullNode.getInstance() : params.deepCopy();
    }

    @Override
    public JsonNode params() {
        return params.deepCopy();
    }
}
