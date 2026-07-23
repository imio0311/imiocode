package io.imiocode.llm.provider.deepseek;

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

    private static DeepSeekClient client(MockLlmServer server) {
        AppConfig config = new AppConfig(Provider.DEEPSEEK, "test-model", "test-key", server.baseUri(),
                Duration.ofSeconds(2), Duration.ofSeconds(5), 321);
        return new DeepSeekClient(config, HttpClient.newHttpClient(), new ObjectMapper(),
                new SseEventReader(), new HttpErrorMapper());
    }

    private static ChatRequest request() {
        return new ChatRequest(List.of(new ChatMessage(MessageRole.USER, "你好")));
    }
}
