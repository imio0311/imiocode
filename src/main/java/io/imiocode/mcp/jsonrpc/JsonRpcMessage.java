package io.imiocode.mcp.jsonrpc;

/** JSON-RPC 2.0 消息基类。 */
public sealed interface JsonRpcMessage
        permits JsonRpcRequest, JsonRpcResponse, JsonRpcNotification {
}
