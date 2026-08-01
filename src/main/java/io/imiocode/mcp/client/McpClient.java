package io.imiocode.mcp.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.mcp.jsonrpc.JsonRpcId;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** ImioCode 只消费 MCP Tools 所需的 Client 接口。 */
public interface McpClient extends AutoCloseable {
    CompletableFuture<McpInitializeResult> connect();

    CompletableFuture<List<McpRemoteTool>> listTools();

    McpCallHandle callTool(String toolName, ObjectNode arguments, Duration timeout);

    CompletableFuture<Void> cancelRequest(JsonRpcId requestId, String reason);

    McpClientState state();

    @Override
    void close();
}
