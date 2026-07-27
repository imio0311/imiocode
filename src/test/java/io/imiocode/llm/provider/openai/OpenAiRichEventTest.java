package io.imiocode.llm.provider.openai;

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
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.OpenAiReasoningMetadata;
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

class OpenAiRichEventTest {
    @Test
    void mapsReasoningSummaryEncryptedContentAndUsage() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("""
                    event: response.output_item.added
                    data: {"type":"response.output_item.added","output_index":0,"item":{"type":"reasoning","id":"rs_1"}}

                    event: response.reasoning_summary_text.delta
                    data: {"type":"response.reasoning_summary_text.delta","output_index":0,"delta":"分析"}

                    event: response.output_item.done
                    data: {"type":"response.output_item.done","output_index":0,"item":{"type":"reasoning","id":"rs_1","encrypted_content":"enc-secret"}}

                    event: response.output_text.delta
                    data: {"type":"response.output_text.delta","delta":"答案"}

                    event: response.completed
                    data: {"type":"response.completed","response":{"status":"completed","usage":{"input_tokens":11,"output_tokens":6,"input_tokens_details":{"cached_tokens":2},"output_tokens_details":{"reasoning_tokens":3}}}}

                    """);
            List<LlmEvent> events = new ArrayList<>();
            ChatRequest request = new ChatRequest(
                    List.of(new ChatMessage(MessageRole.USER, "你好")),
                    List.of(new SystemReminder("仅本轮生效")));

            var response = client(server).streamChat(request, (LlmEventListener) events::add);
            JsonNode body = new ObjectMapper().readTree(server.takeRequest().body());

            ThinkingPart thinking = assertInstanceOf(ThinkingPart.class, response.message().parts().get(0));
            OpenAiReasoningMetadata metadata =
                    assertInstanceOf(OpenAiReasoningMetadata.class, thinking.metadata());
            assertEquals("分析", thinking.text());
            assertEquals("rs_1", metadata.itemId());
            assertEquals("enc-secret", metadata.encryptedContent());
            assertEquals("答案", response.text());
            assertEquals(11, response.usage().inputTokens().orElseThrow());
            assertEquals(6, response.usage().outputTokens().orElseThrow());
            assertEquals(3, response.usage().reasoningTokens().orElseThrow());
            assertEquals(2, response.usage().cacheReadTokens().orElseThrow());
            assertInstanceOf(LlmEvent.StreamCompleted.class, events.getLast());
            assertEquals("high", body.path("reasoning").path("effort").asText());
            assertEquals("detailed", body.path("reasoning").path("summary").asText());
            assertEquals("reasoning.encrypted_content", body.path("include").get(0).asText());
            assertTrue(body.path("instructions").asText().contains("仅本轮生效"));
        }
    }

    @Test
    void restoresReasoningItemHistory() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("""
                    event: response.output_text.delta
                    data: {"type":"response.output_text.delta","delta":"完成"}

                    event: response.completed
                    data: {"type":"response.completed","response":{"status":"completed"}}

                    """);
            ChatRequest request = new ChatRequest(List.of(
                    new ChatMessage(MessageRole.USER, "第一问"),
                    new ChatMessage(
                            MessageRole.ASSISTANT,
                            List.of(
                                    new ThinkingPart(
                                            "分析",
                                            new OpenAiReasoningMetadata("rs-history", "enc-history")),
                                    new TextPart("旧回答"))),
                    new ChatMessage(MessageRole.USER, "继续")));

            client(server).streamChat(request, (LlmEventListener) event -> { });
            JsonNode input = new ObjectMapper()
                    .readTree(server.takeRequest().body())
                    .path("input");

            assertEquals("reasoning", input.get(1).path("type").asText());
            assertEquals("rs-history", input.get(1).path("id").asText());
            assertEquals("enc-history", input.get(1).path("encrypted_content").asText());
            assertEquals("分析", input.get(1).path("summary").get(0).path("text").asText());
            assertEquals("assistant", input.get(2).path("role").asText());
            assertEquals("旧回答", input.get(2).path("content").asText());
        }
    }

    private static OpenAiClient client(MockLlmServer server) {
        ThinkingConfig thinking = new ThinkingConfig(
                true, ThinkingMode.AUTO, 1024, ReasoningEffort.HIGH, ReasoningSummary.DETAILED);
        AppConfig config = new AppConfig(
                Provider.OPENAI,
                "gpt-5",
                "test-key",
                server.baseUri(),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                4096,
                thinking);
        return new OpenAiClient(
                config,
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                new SseEventReader(),
                new HttpErrorMapper(),
                new ToolRegistry());
    }
}
