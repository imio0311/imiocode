package io.imiocode.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.mcp.client.McpCallHandle;
import io.imiocode.mcp.client.McpCallResult;
import io.imiocode.mcp.client.McpClient;
import io.imiocode.mcp.client.McpClientState;
import io.imiocode.mcp.client.McpInitializeResult;
import io.imiocode.mcp.client.McpRemoteTool;
import io.imiocode.mcp.jsonrpc.JsonRpcId;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpToolWrapperTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void exposesHighRiskSchemaAndSanitizesResult() {
        FakeClient client = new FakeClient();
        client.next.complete(new McpCallResult(
                List.of(mapper.createObjectNode().put("type", "text")
                        .put("text", "secret-value")),
                null,
                false));
        SecretRedactor redactor = new SecretRedactor("");
        redactor.registerSecret("secret-value");
        McpToolWrapper wrapper = wrapper(client, redactor);

        var result = wrapper.execute(mapper.createObjectNode().put("query", "x"));

        assertEquals("mcp_server__remote_tool", wrapper.definition().name());
        assertEquals(ToolRisk.HIGH, wrapper.definition().risk());
        assertEquals("object", wrapper.definition().inputSchema().path("type").asText());
        assertTrue(result.success());
        assertEquals("***", result.output());
    }

    @Test
    void cancelIsIdempotentAndPropagates() throws Exception {
        FakeClient client = new FakeClient();
        McpToolWrapper wrapper = wrapper(client, new SecretRedactor(""));
        Thread thread = Thread.ofVirtual().start(() -> wrapper.execute(mapper.createObjectNode()));
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(1);
        while (client.calls.get() == 0 && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }

        wrapper.cancel();
        wrapper.cancel();
        thread.join(Duration.ofSeconds(2));

        assertEquals(1, client.cancellations.get());
        assertFalse(thread.isAlive());
    }

    private McpToolWrapper wrapper(FakeClient client, SecretRedactor redactor) {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        return new McpToolWrapper(
                "server",
                client,
                new McpRemoteTool("remote/tool", "remote", schema),
                Duration.ofSeconds(1),
                ToolLimits.defaults(),
                redactor);
    }

    private static final class FakeClient implements McpClient {
        private final CompletableFuture<McpCallResult> next = new CompletableFuture<>();
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger cancellations = new AtomicInteger();

        @Override
        public CompletableFuture<McpInitializeResult> connect() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<List<McpRemoteTool>> listTools() {
            throw new UnsupportedOperationException();
        }

        @Override
        public McpCallHandle callTool(String toolName, ObjectNode arguments, Duration timeout) {
            calls.incrementAndGet();
            return new McpCallHandle(new JsonRpcId(IntNode.valueOf(1)), next);
        }

        @Override
        public CompletableFuture<Void> cancelRequest(JsonRpcId requestId, String reason) {
            cancellations.incrementAndGet();
            next.cancel(false);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public McpClientState state() {
            return McpClientState.READY;
        }

        @Override
        public void close() {
        }
    }
}
