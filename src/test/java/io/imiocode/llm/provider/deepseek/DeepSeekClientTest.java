package io.imiocode.llm.provider.deepseek;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepSeekClientTest {
    @Test
    void sendsChatCompletionsRequestAndStreamsText() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("data: {\"choices\":[{\"delta\":{}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{\"content\":\"你\"}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{\"content\":\"好\"},\"finish_reason\":\"stop\"}]}\n\n"
                    + "data: [DONE]\n\n");
            DeepSeekClient client = client(server);
            List<String> deltas = new ArrayList<>();

            String response = client.streamChat(request(), deltas::add).content();
            MockLlmServer.RecordedRequest recorded = server.takeRequest();
            JsonNode body = new ObjectMapper().readTree(recorded.body());

            assertEquals("你好", response);
            assertEquals(List.of("你", "好"), deltas);
            assertEquals("/chat/completions", recorded.uri().getPath());
            assertEquals("Bearer test-key", recorded.firstHeader("Authorization"));
            assertEquals(true, body.path("stream").asBoolean());
            assertEquals(321, body.path("max_tokens").asInt());
        }
    }

    @Test
    void rejectsMissingChoices() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("data: {\"id\":\"bad\"}\n\n");

            LlmException exception = assertThrows(LlmException.class,
                    () -> client(server).streamChat(request(), text -> { }));

            assertEquals(LlmErrorType.PROTOCOL, exception.type());
        }
    }

    @Test
    void mapsServerFailure() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueue(500, "application/json", List.of("{\"error\":{\"code\":\"server_error\"}}"), 0);

            LlmException exception = assertThrows(LlmException.class,
                    () -> client(server).streamChat(request(), text -> { }));

            assertEquals(LlmErrorType.SERVER_ERROR, exception.type());
        }
    }

    @Test
    void parsesInterleavedToolCallsAndMapsToolMessages() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"content\":\"正在处理\",\"tool_calls\":["
                    + "{\"index\":1,\"id\":\"c2\",\"function\":{\"name\":\"grep\",\"arguments\":\"{\\\"pattern\\\":\"}},"
                    + "{\"index\":0,\"id\":\"c1\",\"function\":{\"name\":\"read_file\",\"arguments\":\"{\\\"path\\\":\\\"a\"}}]}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{\"tool_calls\":["
                    + "{\"index\":0,\"function\":{\"arguments\":\".txt\\\"}\"}},"
                    + "{\"index\":1,\"function\":{\"arguments\":\"\\\"中文\\\"}\"}}]},"
                    + "\"finish_reason\":\"tool_calls\"}]}\n\n"
                    + "data: [DONE]\n\n");
            ToolRegistry registry = registry();

            var response = client(server, registry).streamChat(toolHistory(), text -> { });
            JsonNode body = new ObjectMapper().readTree(server.takeRequest().body());

            assertEquals(List.of("c1", "c2"), response.toolCalls().stream().map(ToolCall::id).toList());
            assertEquals("正在处理", response.text());
            assertEquals(1, body.path("tools").size());
            assertEquals("read_file", body.path("tools").get(0).path("function").path("name").asText());
            assertEquals("assistant", body.path("messages").get(1).path("role").asText());
            assertEquals("c1", body.path("messages").get(1).path("tool_calls").get(0).path("id").asText());
            assertEquals("tool", body.path("messages").get(2).path("role").asText());
            assertEquals("c1", body.path("messages").get(2).path("tool_call_id").asText());
        }
    }

    @Test
    void rejectsInvalidToolJson() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"tool_calls\":["
                    + "{\"index\":0,\"id\":\"c1\",\"function\":{\"name\":\"read_file\",\"arguments\":\"{\"}}]},"
                    + "\"finish_reason\":\"tool_calls\"}]}\n\n"
                    + "data: [DONE]\n\n");

            assertThrows(LlmException.class, () -> client(server).streamChat(request(), text -> { }));
        }
    }

    private static DeepSeekClient client(MockLlmServer server) {
        return client(server, new ToolRegistry());
    }

    private static DeepSeekClient client(MockLlmServer server, ToolRegistry registry) {
        AppConfig config = new AppConfig(Provider.DEEPSEEK, "test-model", "test-key", server.baseUri(),
                Duration.ofSeconds(2), Duration.ofSeconds(5), 321);
        return new DeepSeekClient(config, HttpClient.newHttpClient(), new ObjectMapper(),
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
                        List.of(new ToolResultPart("c1", "read_file", ToolResult.success("ok"))))));
    }

    private static ToolRegistry registry() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(stub("read_file"));
        registry.register(stub("write_file"));
        registry.disable("write_file");
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
