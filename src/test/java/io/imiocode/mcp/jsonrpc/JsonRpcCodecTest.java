package io.imiocode.mcp.jsonrpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRpcCodecTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonRpcCodec codec = new JsonRpcCodec(mapper);

    @Test
    void roundTripsAllMessageKinds() {
        JsonRpcRequest request = new JsonRpcRequest(
                new JsonRpcId(IntNode.valueOf(1)),
                "tools/list",
                mapper.createObjectNode().put("cursor", "next"));
        JsonRpcRequest decodedRequest = assertInstanceOf(
                JsonRpcRequest.class,
                codec.decode(codec.encode(request)));
        assertEquals(request.id(), decodedRequest.id());
        assertEquals("next", decodedRequest.params().path("cursor").asText());

        JsonRpcResponse success = new JsonRpcResponse(
                new JsonRpcId(TextNode.valueOf("abc")),
                mapper.createObjectNode().put("ok", true),
                null);
        assertTrue(assertInstanceOf(
                JsonRpcResponse.class,
                codec.decode(codec.encode(success))).successful());

        JsonRpcResponse failure = new JsonRpcResponse(
                new JsonRpcId(IntNode.valueOf(2)),
                null,
                new JsonRpcError(-32601, "Method not found", mapper.createObjectNode()));
        assertEquals(-32601, assertInstanceOf(
                JsonRpcResponse.class,
                codec.decode(codec.encode(failure))).error().code());

        JsonRpcNotification notification = new JsonRpcNotification(
                "notifications/initialized",
                null);
        assertEquals("notifications/initialized", assertInstanceOf(
                JsonRpcNotification.class,
                codec.decode(codec.encode(notification))).method());
    }

    @Test
    void ignoresUnknownFieldsButRejectsMalformedMessages() {
        JsonRpcRequest request = assertInstanceOf(
                JsonRpcRequest.class,
                codec.decode("""
                        {"jsonrpc":"2.0","id":1,"method":"ping","future":true}
                        """));
        assertEquals("ping", request.method());

        assertThrows(IllegalArgumentException.class, () -> codec.decode("{}"));
        assertThrows(IllegalArgumentException.class, () -> codec.decode("""
                {"jsonrpc":"1.0","id":1,"result":{}}
                """));
        assertThrows(IllegalArgumentException.class, () -> codec.decode("""
                {"jsonrpc":"2.0","id":1,"result":{},"error":{"code":1,"message":"bad"}}
                """));
        assertThrows(IllegalArgumentException.class, () -> codec.decode("""
                {"jsonrpc":"2.0","id":true,"result":{}}
                """));
        assertThrows(IllegalArgumentException.class, () -> codec.decode("secret-not-json"));
    }
}
