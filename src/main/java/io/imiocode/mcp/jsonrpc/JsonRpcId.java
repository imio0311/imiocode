package io.imiocode.mcp.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/** JSON-RPC 字符串或整数 ID。 */
public record JsonRpcId(JsonNode value) {
    public JsonRpcId {
        value = Objects.requireNonNull(value, "value").deepCopy();
        if (!value.isTextual() && !value.isIntegralNumber()) {
            throw new IllegalArgumentException("JSON-RPC id 必须是字符串或整数");
        }
    }

    @Override
    public JsonNode value() {
        return value.deepCopy();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof JsonRpcId that)) {
            return false;
        }
        if (value.isIntegralNumber() && that.value.isIntegralNumber()) {
            return value.bigIntegerValue().equals(that.value.bigIntegerValue());
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.isIntegralNumber()
                ? value.bigIntegerValue().hashCode()
                : value.hashCode();
    }
}
