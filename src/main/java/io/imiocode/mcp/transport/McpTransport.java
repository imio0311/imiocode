package io.imiocode.mcp.transport;

import io.imiocode.mcp.jsonrpc.JsonRpcMessage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** MCP Client 与具体通信介质之间的统一异步边界。 */
public interface McpTransport extends AutoCloseable {
    CompletableFuture<Void> start(
            Consumer<JsonRpcMessage> inboundHandler,
            Consumer<Throwable> failureHandler);

    CompletableFuture<Void> send(JsonRpcMessage message);

    boolean isOpen();

    @Override
    void close();
}
