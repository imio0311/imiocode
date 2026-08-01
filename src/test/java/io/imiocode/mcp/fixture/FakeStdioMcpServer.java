package io.imiocode.mcp.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.jsonrpc.JsonRpcRequest;
import io.imiocode.mcp.jsonrpc.JsonRpcResponse;
import io.imiocode.mcp.jsonrpc.JsonRpcNotification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.TreeSet;

/** 仅供测试启动的真实 Java stdio MCP Server。 */
public final class FakeStdioMcpServer {
    private FakeStdioMcpServer() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "normal" : args[0];
        JsonRpcCodec codec = new JsonRpcCodec();
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(
                     new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
            String line;
            while ((line = input.readLine()) != null) {
                if ("malformed".equals(mode)) {
                    output.write("not-json\n");
                    output.flush();
                    return;
                }
                var message = codec.decode(line);
                if (message instanceof JsonRpcNotification) {
                    continue;
                }
                JsonRpcRequest request = (JsonRpcRequest) message;
                ObjectNode result = mapper.createObjectNode();
                if ("environment".equals(request.method())) {
                    var names = result.putArray("names");
                    new TreeSet<>(System.getenv().keySet()).forEach(names::add);
                    result.put("explicit", System.getenv().getOrDefault("EXPLICIT_TEST", ""));
                } else if ("normal".equals(mode) && "initialize".equals(request.method())) {
                    result.put("protocolVersion", "2025-11-25");
                    result.putObject("capabilities").putObject("tools");
                    result.putObject("serverInfo").put("name", "fake-stdio").put("version", "1.0");
                    result.put("instructions", "MALICIOUS_INSTRUCTIONS_SENTINEL");
                } else if ("normal".equals(mode) && "tools/list".equals(request.method())) {
                    result.putArray("tools").addObject()
                            .put("name", "echo/text")
                            .put("description", "回显传入文本")
                            .putObject("inputSchema")
                            .put("type", "object")
                            .putObject("properties")
                            .putObject("text")
                            .put("type", "string");
                } else if ("normal".equals(mode) && "tools/call".equals(request.method())) {
                    result.putArray("content").addObject()
                            .put("type", "text")
                            .put("text", request.params().path("arguments").path("text").asText(""));
                } else {
                    result.put("method", request.method());
                    result.set("params", request.params());
                }
                if ("stderr".equals(mode)) {
                    System.err.print("x".repeat(80_000));
                    System.err.flush();
                }
                output.write(codec.encode(new JsonRpcResponse(request.id(), result, null)));
                output.newLine();
                output.flush();
                if ("exit".equals(mode)) {
                    return;
                }
            }
        }
    }
}
