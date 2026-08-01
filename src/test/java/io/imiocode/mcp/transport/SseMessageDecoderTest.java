package io.imiocode.mcp.transport;

import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.jsonrpc.JsonRpcResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseMessageDecoderTest {
    @Test
    void decodesMultipleEventsCommentsAndFinalEvent() {
        SseMessageDecoder decoder = new SseMessageDecoder(new JsonRpcCodec());
        byte[] body = """
                : priming

                event: message
                data: {"jsonrpc":"2.0","id":1,"result":{"ok":true}}

                data: {"jsonrpc":"2.0","id":"two","result":
                data: {"value":2}}
                """.getBytes(StandardCharsets.UTF_8);

        var messages = decoder.decode(body);

        assertEquals(2, messages.size());
        assertTrue(((JsonRpcResponse) messages.getFirst()).result().path("ok").asBoolean());
        assertEquals(2, ((JsonRpcResponse) messages.get(1)).result().path("value").asInt());
    }

    @Test
    void ignoresEmptyDataAndRejectsMalformedOrOversizedBody() {
        SseMessageDecoder decoder = new SseMessageDecoder(new JsonRpcCodec(), 100, 2);
        assertTrue(decoder.decode("data:\n\n".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertThrows(McpTransportException.class, () ->
                decoder.decode(new byte[101]));
        assertThrows(IllegalArgumentException.class, () ->
                decoder.decode("data: not-json\n\n".getBytes(StandardCharsets.UTF_8)));
    }
}
