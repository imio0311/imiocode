package io.imiocode.mcp.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

/** JSON-RPC 标准错误对象。 */
public record JsonRpcError(int code, String message, JsonNode data) {
    public JsonRpcError {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("错误 message 不能为空");
        }
        message = message.trim();
        data = data == null ? NullNode.getInstance() : data.deepCopy();
    }

    @Override
    public JsonNode data() {
        return data.deepCopy();
    }
}
