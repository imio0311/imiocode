package io.imiocode.llm.provider.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.config.AppConfig;
import io.imiocode.config.Provider;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.TextPart;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiClientTest {
    @Test
    void sendsResponsesRequestAndStreamsText() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: response.output_text.delta\n"
                    + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"你\"}\n\n"
                    + "event: response.output_text.delta\n"
                    + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"好\"}\n\n"
                    + "event: response.completed\n"
                    + "data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}\n\n");
            OpenAiClient client = client(server);
            List<String> deltas = new ArrayList<>();

            String response = client.streamChat(request(), deltas::add).content();
            MockLlmServer.RecordedRequest recorded = server.takeRequest();
            JsonNode body = new ObjectMapper().readTree(recorded.body());

            assertEquals("你好", response);
            assertEquals(List.of("你", "好"), deltas);
            assertEquals("/v1/responses", recorded.uri().getPath());
            assertEquals("Bearer test-key", recorded.firstHeader("Authorization"));
            assertEquals("test-model", body.path("model").asText());
            assertEquals(true, body.path("stream").asBoolean());
            assertEquals(321, body.path("max_output_tokens").asInt());
            assertEquals("assistant", body.path("input").get(1).path("role").asText());
        }
    }

    @Test
    void rejectsStreamWithoutCompletedEvent() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: response.output_text.delta\n"
                    + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"partial\"}\n\n");

            LlmException exception = assertThrows(LlmException.class,
                    () -> client(server).streamChat(request(), text -> { }));

            assertEquals(LlmErrorType.PROTOCOL, exception.type());
        }
    }

    @Test
    void mapsAuthenticationFailure() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueue(401, "application/json", List.of("{\"error\":{\"code\":\"invalid_api_key\"}}"), 0);

            LlmException exception = assertThrows(LlmException.class,
                    () -> client(server).streamChat(request(), text -> { }));

            assertEquals(LlmErrorType.AUTHENTICATION, exception.type());
        }
    }

    @Test
    void parsesInterleavedToolCallsAndMapsToolHistory() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: response.output_item.added\n"
                    + "data: {\"type\":\"response.output_item.added\",\"output_index\":1,"
                    + "\"item\":{\"type\":\"function_call\",\"call_id\":\"c2\",\"name\":\"grep\"}}\n\n"
                    + "event: response.output_item.added\n"
                    + "data: {\"type\":\"response.output_item.added\",\"output_index\":0,"
                    + "\"item\":{\"type\":\"function_call\",\"call_id\":\"c1\",\"name\":\"read_file\"}}\n\n"
                    + "event: response.function_call_arguments.delta\n"
                    + "data: {\"type\":\"response.function_call_arguments.delta\",\"output_index\":1,"
                    + "\"delta\":\"{\\\"pattern\\\":\\\"中文\\\"}\"}\n\n"
                    + "event: response.function_call_arguments.delta\n"
                    + "data: {\"type\":\"response.function_call_arguments.delta\",\"output_index\":0,"
                    + "\"delta\":\"{\\\"path\\\":\\\"a.txt\\\"}\"}\n\n"
                    + "event: response.function_call_arguments.delta\n"
                    + "data: {\"type\":\"response.function_call_arguments.delta\",\"output_index\":1,"
                    + "\"delta\":\"\"}\n\n"
                    + "event: response.output_text.delta\n"
                    + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"正在处理\"}\n\n"
                    + "event: response.completed\n"
                    + "data: {\"type\":\"response.completed\"}\n\n");
            ToolRegistry registry = registry();
            OpenAiClient client = client(server, registry);

            var response = client.streamChat(toolHistory(), text -> { });
            JsonNode body = new ObjectMapper().readTree(server.takeRequest().body());

            assertEquals(List.of("c1", "c2"), response.toolCalls().stream().map(ToolCall::id).toList());
            assertEquals("正在处理", response.text());
            assertEquals(1, body.path("tools").size());
            assertEquals("read_file", body.path("tools").get(0).path("name").asText());
            assertEquals("function_call", body.path("input").get(1).path("type").asText());
            assertEquals("function_call_output", body.path("input").get(2).path("type").asText());
            assertEquals("c1", body.path("input").get(2).path("call_id").asText());
        }
    }

    @Test
    void rejectsInvalidToolArgumentsAtCompletion() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: response.output_item.added\n"
                    + "data: {\"type\":\"response.output_item.added\",\"output_index\":0,"
                    + "\"item\":{\"type\":\"function_call\",\"call_id\":\"c1\",\"name\":\"read_file\"}}\n\n"
                    + "event: response.function_call_arguments.delta\n"
                    + "data: {\"type\":\"response.function_call_arguments.delta\",\"output_index\":0,\"delta\":\"{\"}\n\n"
                    + "event: response.completed\n"
                    + "data: {\"type\":\"response.completed\"}\n\n");

            assertThrows(LlmException.class, () -> client(server).streamChat(request(), text -> { }));
        }
    }

    @Test
    void closeCancelsAnActiveStream() throws Exception {
        try (MockLlmServer server = new MockLlmServer();
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            server.enqueue(200, "text/event-stream", List.of(
                    "event: response.output_text.delta\n"
                            + "data: {\"type\":\"response.output_text.delta\",\"delta\":\"partial\"}\n\n",
                    "event: response.completed\n"
                            + "data: {\"type\":\"response.completed\"}\n\n"), 5_000);
            OpenAiClient client = client(server);
            CountDownLatch firstDelta = new CountDownLatch(1);

            var future = executor.submit(() -> {
                try {
                    client.streamChat(request(), text -> firstDelta.countDown());
                } catch (LlmException exception) {
                    throw new RuntimeException(exception);
                }
            });
            assertTrue(firstDelta.await(2, TimeUnit.SECONDS));
            client.close();

            ExecutionException failure = assertThrows(
                    ExecutionException.class,
                    () -> future.get(2, TimeUnit.SECONDS));
            LlmException exception = (LlmException) failure.getCause().getCause();
            assertEquals(LlmErrorType.INTERRUPTED, exception.type());
        }
    }

    private static OpenAiClient client(MockLlmServer server) {
        return client(server, new ToolRegistry());
    }

    private static OpenAiClient client(MockLlmServer server, ToolRegistry registry) {
        AppConfig config = new AppConfig(Provider.OPENAI, "test-model", "test-key", server.baseUri(),
                Duration.ofSeconds(2), Duration.ofSeconds(5), 321);
        return new OpenAiClient(config, HttpClient.newHttpClient(), new ObjectMapper(),
                new SseEventReader(), new HttpErrorMapper(), registry);
    }

    private static ChatRequest request() {
        return new ChatRequest(List.of(
                new ChatMessage(MessageRole.USER, "你好"),
                new ChatMessage(MessageRole.ASSISTANT, "你好，有什么可以帮你？"),
                new ChatMessage(MessageRole.USER, "继续")));
    }

    private static ChatRequest toolHistory() {
        ToolCall call = new ToolCall("c1", "read_file",
                new ObjectMapper().createObjectNode().put("path", "old.txt"));
        return new ChatRequest(List.of(
                new ChatMessage(MessageRole.USER, "读取"),
                new ChatMessage(MessageRole.ASSISTANT,
                        List.of(new ToolCallPart(call))),
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
