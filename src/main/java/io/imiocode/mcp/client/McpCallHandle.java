package io.imiocode.mcp.client;

import io.imiocode.mcp.jsonrpc.JsonRpcId;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** 远程工具调用的请求 ID 与可等待结果。 */
public record McpCallHandle(
        JsonRpcId requestId,
        CompletableFuture<McpCallResult> result) {
    public McpCallHandle {
        requestId = Objects.requireNonNull(requestId, "requestId");
        result = Objects.requireNonNull(result, "result");
    }
}
