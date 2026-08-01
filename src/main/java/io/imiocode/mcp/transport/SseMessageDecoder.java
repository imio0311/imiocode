package io.imiocode.mcp.transport;

import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.jsonrpc.JsonRpcMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 仅解码单次 HTTP POST 内结束的有限 SSE 响应。 */
public final class SseMessageDecoder {
    private static final int DEFAULT_MAX_BYTES = 4 * 1024 * 1024;
    private static final int DEFAULT_MAX_MESSAGES = 1_000;

    private final JsonRpcCodec codec;
    private final int maxBytes;
    private final int maxMessages;

    public SseMessageDecoder(JsonRpcCodec codec) {
        this(codec, DEFAULT_MAX_BYTES, DEFAULT_MAX_MESSAGES);
    }

    SseMessageDecoder(JsonRpcCodec codec, int maxBytes, int maxMessages) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.maxBytes = maxBytes;
        this.maxMessages = maxMessages;
    }

    public List<JsonRpcMessage> decode(byte[] response) {
        Objects.requireNonNull(response, "response");
        if (response.length > maxBytes) {
            throw new McpTransportException("MCP SSE 响应超过大小限制");
        }
        String text = new String(response, StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        List<JsonRpcMessage> messages = new ArrayList<>();
        StringBuilder data = new StringBuilder();
        for (String line : (text + "\n").split("\n", -1)) {
            if (line.isEmpty()) {
                addEvent(data, messages);
                data.setLength(0);
                continue;
            }
            if (line.startsWith(":") || !line.startsWith("data:")) {
                continue;
            }
            String value = line.substring(5);
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }
            if (!data.isEmpty()) {
                data.append('\n');
            }
            data.append(value);
        }
        return List.copyOf(messages);
    }

    private void addEvent(StringBuilder data, List<JsonRpcMessage> messages) {
        if (data.isEmpty()) {
            return;
        }
        if (messages.size() >= maxMessages) {
            throw new McpTransportException("MCP SSE 消息数量超过限制");
        }
        messages.add(codec.decode(data.toString()));
    }
}
