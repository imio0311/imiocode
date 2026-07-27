package io.imiocode.llm.provider.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.config.AppConfig;
import io.imiocode.config.Provider;
import io.imiocode.config.ReasoningEffort;
import io.imiocode.config.ReasoningSummary;
import io.imiocode.config.ThinkingConfig;
import io.imiocode.config.ThinkingMode;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.DeepSeekReasoningMetadata;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.transport.HttpErrorMapper;
import io.imiocode.llm.transport.MockLlmServer;
import io.imiocode.llm.transport.SseEventReader;
import io.imiocode.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekRichEventTest {
    @Test
    void mapsReasoningContentUsageAndReminder() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("""
                    data: {"choices":[{"delta":{"reasoning_content":"分析"}}]}

                    data: {"choices":[{"delta":{"content":"答案"},"finish_reason":"stop"}]}

                    data: {"choices":[],"usage":{"prompt_tokens":10,"completion_tokens":6,"prompt_tokens_details":{"cached_tokens":1},"completion_tokens_details":{"reasoning_tokens":2}}}

                    data: [DONE]

                    """);
            List<LlmEvent> events = new ArrayList<>();
            ChatRequest request = new ChatRequest(
                    List.of(new ChatMessage(MessageRole.USER, "你好")),
                    List.of(new SystemReminder("仅本轮生效")));

            var response = client(server).streamChat(request, (LlmEventListener) events::add);
            JsonNode body = new ObjectMapper().readTree(server.takeRequest().body());

            ThinkingPart thinking = assertInstanceOf(ThinkingPart.class, response.message().parts().get(0));
            assertInstanceOf(DeepSeekReasoningMetadata.class, thinking.metadata());
            assertEquals("分析", thinking.text());
            assertEquals("答案", response.text());
            assertEquals(10, response.usage().inputTokens().orElseThrow());
            assertEquals(6, response.usage().outputTokens().orElseThrow());
            assertEquals(2, response.usage().reasoningTokens().orElseThrow());
            assertEquals(1, response.usage().cacheReadTokens().orElseThrow());
            assertInstanceOf(LlmEvent.StreamCompleted.class, events.getLast());
            assertEquals("enabled", body.path("thinking").path("type").asText());
            assertEquals("medium", body.path("reasoning_effort").asText());
            assertEquals("system", body.path("messages").get(0).path("role").asText());
            assertTrue(body.path("messages").get(0).path("content").asText().contains("仅本轮生效"));
        }
    }

    @Test
    void restoresReasoningContentHistory() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("""
                    data: {"choices":[{"delta":{"content":"完成"},"finish_reason":"stop"}]}

                    data: [DONE]

                    """);
            ChatRequest request = new ChatRequest(List.of(
                    new ChatMessage(MessageRole.USER, "第一问"),
                    new ChatMessage(
                            MessageRole.ASSISTANT,
                            List.of(
                                    new ThinkingPart("分析", new DeepSeekReasoningMetadata()),
                                    new TextPart("旧回答"))),
                    new ChatMessage(MessageRole.USER, "继续")));

            client(server).streamChat(request, (LlmEventListener) event -> { });
            JsonNode messages = new ObjectMapper()
                    .readTree(server.takeRequest().body())
                    .path("messages");

            assertEquals("assistant", messages.get(1).path("role").asText());
            assertEquals("旧回答", messages.get(1).path("content").asText());
            assertEquals("分析", messages.get(1).path("reasoning_content").asText());
        }
    }

    private static DeepSeekClient client(MockLlmServer server) {
        ThinkingConfig thinking = new ThinkingConfig(
                true, ThinkingMode.AUTO, 1024, ReasoningEffort.MEDIUM, ReasoningSummary.AUTO);
        AppConfig config = new AppConfig(
                Provider.DEEPSEEK,
                "deepseek-reasoner",
                "test-key",
                server.baseUri(),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                4096,
                thinking);
        return new DeepSeekClient(
                config,
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                new SseEventReader(),
                new HttpErrorMapper(),
                new ToolRegistry());
    }
}
