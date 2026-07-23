package io.imiocode.llm.provider.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.config.AppConfig;
import io.imiocode.config.Provider;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.transport.HttpErrorMapper;
import io.imiocode.llm.transport.MockLlmServer;
import io.imiocode.llm.transport.SseEventReader;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnthropicClientTest {
    @Test
    void sendsMessagesRequestAndStreamsTextOnly() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("event: content_block_delta\n"
                    + "data: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{}\"}}\n\n"
                    + "event: content_block_delta\n"
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
    void mapsRateLimitFailure() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueue(429, "application/json", List.of("{\"error\":{\"type\":\"rate_limit_error\"}}"), 0);

            LlmException exception = assertThrows(LlmException.class,
                    () -> client(server).streamChat(request(), text -> { }));

            assertEquals(LlmErrorType.RATE_LIMIT, exception.type());
        }
    }

    private static AnthropicClient client(MockLlmServer server) {
        AppConfig config = new AppConfig(Provider.ANTHROPIC, "test-model", "test-key", server.baseUri(),
                Duration.ofSeconds(2), Duration.ofSeconds(5), 321);
        return new AnthropicClient(config, HttpClient.newHttpClient(), new ObjectMapper(),
                new SseEventReader(), new HttpErrorMapper());
    }

    private static ChatRequest request() {
        return new ChatRequest(List.of(new ChatMessage(MessageRole.USER, "你好")));
    }
}
