package io.imiocode.llm.provider.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.config.AppConfig;
import io.imiocode.config.Provider;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.transport.HttpErrorMapper;
import io.imiocode.llm.transport.MockLlmServer;
import io.imiocode.llm.transport.SseEventReader;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnthropicClientTest {
    @Test
    void sendsMessagesRequestAndStreamsTextOnly() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"你\"}}\n\n"
                    + "event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"好\"}}\n\n"
                    + "event: message_stop\n"
                    + "data: {\"type\":\"message_stop\"}\n\n");
            AnthropicClient client = client(server);
            List<String> deltas = new ArrayList<>();

            String response = client.streamChat(request(), deltas::add).content();
            MockLlmServer.RecordedRequest recorded = server.takeRequest();
            JsonNode body = new ObjectMapper().readTree(recorded.body());

            assertEquals("你好", response);
            assertEquals(List.of("你", "好"), deltas);
            assertEquals("/v1/messages", recorded.uri().getPath());
            assertEquals("test-key", recorded.firstHeader("x-api-key"));
            assertEquals("2023-06-01", recorded.firstHeader("anthropic-version"));
            assertEquals(321, body.path("max_tokens").asInt());
        }
    }

    @Test
    void rejectsStreamWithoutMessageStop() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"partial\"}}\n\n");

            LlmException exception = assertThrows(LlmException.class,
                    () -> client(server).streamChat(request(), text -> { }));

            assertEquals(LlmErrorType.PROTOCOL, exception.type());
        }
    }

    @Test
    void mapsOutputLimitAndUsesRequestOverride() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: message_delta\n"
                    + "data: {\"type\":\"message_delta\",\"delta\":"
                    + "{\"stop_reason\":\"max_tokens\"},\"usage\":{}}\n\n");
            ChatRequest overridden = new ChatRequest(
                    request().messages(),
                    request().reminders(),
                    request().toolSelection(),
                    OptionalInt.of(999));

            LlmException exception = assertThrows(LlmException.class,
                    () -> client(server).streamChat(overridden, text -> { }));
            JsonNode body = new ObjectMapper().readTree(server.takeRequest().body());

            assertEquals(LlmErrorType.OUTPUT_LIMIT, exception.type());
            assertEquals(999, body.path("max_tokens").asInt());
        }
    }

    @Test
    void mapsRateLimitFailure() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueue(429, "application/json", List.of("{\"error\":{\"type\":\"rate_limit_error\"}}"), 0);

            LlmException exception = assertThrows(LlmException.class,
                    () -> client(server).streamChat(request(), text -> { }));

            assertEquals(LlmErrorType.RATE_LIMIT, exception.type());
        }
    }

    @Test
    void parsesToolUseFragmentsAndMapsToolResults() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: content_block_start\n"
                    + "data: {\"type\":\"content_block_start\",\"index\":1,"
                    + "\"content_block\":{\"type\":\"tool_use\",\"id\":\"c2\",\"name\":\"grep\",\"input\":{}}}\n\n"
                    + "event: content_block_start\n"
                    + "data: {\"type\":\"content_block_start\",\"index\":0,"
                    + "\"content_block\":{\"type\":\"tool_use\",\"id\":\"c1\",\"name\":\"read_file\",\"input\":{}}}\n\n"
                    + "event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"index\":1,"
                    + "\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"pattern\\\":\\\"中文\\\"}\"}}\n\n"
                    + "event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"index\":0,"
                    + "\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"path\\\":\\\"a.txt\\\"}\"}}\n\n"
                    + "event: content_block_stop\n"
                    + "data: {\"type\":\"content_block_stop\",\"index\":0}\n\n"
                    + "event: content_block_stop\n"
                    + "data: {\"type\":\"content_block_stop\",\"index\":1}\n\n"
                    + "event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"index\":2,"
                    + "\"delta\":{\"type\":\"text_delta\",\"text\":\"正在处理\"}}\n\n"
                    + "event: message_stop\n"
                    + "data: {\"type\":\"message_stop\"}\n\n");
            ToolRegistry registry = registry();

            var response = client(server, registry).streamChat(toolHistory(), text -> { });
            JsonNode body = new ObjectMapper().readTree(server.takeRequest().body());

            assertEquals(List.of("c1", "c2"), response.toolCalls().stream().map(ToolCall::id).toList());
            assertEquals("正在处理", response.text());
            assertEquals(2, body.path("tools").size());
            assertTrue(body.path("tools").get(0).path("cache_control").isMissingNode());
            assertEquals("ephemeral",
                    body.path("tools").get(1).path("cache_control").path("type").asText());
            assertEquals("tool_use", body.path("messages").get(1).path("content").get(0).path("type").asText());
            assertEquals("tool_result", body.path("messages").get(2).path("content").get(0).path("type").asText());
            assertEquals("user", body.path("messages").get(2).path("role").asText());
            assertEquals(true, body.path("messages").get(2).path("content").get(0).path("is_error").asBoolean());
        }
    }

    @Test
    void rejectsInvalidToolJson() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: content_block_start\n"
                    + "data: {\"type\":\"content_block_start\",\"index\":0,"
                    + "\"content_block\":{\"type\":\"tool_use\",\"id\":\"c1\",\"name\":\"read_file\",\"input\":{}}}\n\n"
                    + "event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"index\":0,"
                    + "\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\"}}\n\n"
                    + "event: content_block_stop\n"
                    + "data: {\"type\":\"content_block_stop\",\"index\":0}\n\n"
                    + "event: message_stop\n"
                    + "data: {\"type\":\"message_stop\"}\n\n");

            assertThrows(LlmException.class, () -> client(server).streamChat(request(), text -> { }));
        }
    }

    private static AnthropicClient client(MockLlmServer server) {
        return client(server, new ToolRegistry());
    }

    private static AnthropicClient client(MockLlmServer server, ToolRegistry registry) {
        AppConfig config = new AppConfig(Provider.ANTHROPIC, "test-model", "test-key", server.baseUri(),
                Duration.ofSeconds(2), Duration.ofSeconds(5), 321);
        return new AnthropicClient(config, HttpClient.newHttpClient(), new ObjectMapper(),
                new SseEventReader(), new HttpErrorMapper(), registry);
    }

    private static ChatRequest request() {
        return new ChatRequest(List.of(new ChatMessage(MessageRole.USER, "你好")));
    }

    private static ChatRequest toolHistory() {
        ToolCall call = new ToolCall("c1", "read_file",
                new ObjectMapper().createObjectNode().put("path", "old.txt"));
        return new ChatRequest(List.of(
                new ChatMessage(MessageRole.USER, "读取"),
                new ChatMessage(MessageRole.ASSISTANT, List.of(new ToolCallPart(call))),
                new ChatMessage(MessageRole.TOOL,
                        List.of(new ToolResultPart("c1", "read_file", ToolResult.failure("失败"))))));
    }

    private static ToolRegistry registry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(stub("read_file"));
        registry.register(stub("write_file"));
        return registry;
    }

    private static Tool stub(String name) {
        return new Tool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(name, name,
                        new ObjectMapper().createObjectNode().put("type", "object"), ToolRisk.LOW);
            }

            @Override
            public ToolResult execute(com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                return ToolResult.success("ok");
            }
        };
    }
}
