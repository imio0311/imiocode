package io.imiocode.llm.provider.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.config.AppConfig;
import io.imiocode.config.Provider;
import io.imiocode.config.ReasoningEffort;
import io.imiocode.config.ReasoningSummary;
import io.imiocode.config.ThinkingConfig;
import io.imiocode.config.ThinkingMode;
import io.imiocode.conversation.AnthropicThinkingMetadata;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.transport.HttpErrorMapper;
import io.imiocode.llm.transport.MockLlmServer;
import io.imiocode.llm.transport.SseEventReader;
import io.imiocode.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnthropicRichEventTest {
    @Test
    void mapsThinkingUsageAndReminder() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("""
                    event: message_start
                    data: {"type":"message_start","message":{"usage":{"input_tokens":12,"cache_read_input_tokens":3,"cache_creation_input_tokens":4}}}

                    event: content_block_start
                    data: {"type":"content_block_start","index":0,"content_block":{"type":"thinking","thinking":"","signature":""}}

                    event: content_block_delta
                    data: {"type":"content_block_delta","index":0,"delta":{"type":"thinking_delta","thinking":"分析"}}

                    event: content_block_delta
                    data: {"type":"content_block_delta","index":0,"delta":{"type":"signature_delta","signature":"sig-secret"}}

                    event: content_block_stop
                    data: {"type":"content_block_stop","index":0}

                    event: content_block_delta
                    data: {"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"答案"}}

                    event: message_delta
                    data: {"type":"message_delta","usage":{"output_tokens":7,"output_tokens_details":{"thinking_tokens":2}}}

                    event: message_stop
                    data: {"type":"message_stop"}

                    """);
            List<LlmEvent> events = new ArrayList<>();
            ChatRequest request = new ChatRequest(
                    List.of(new ChatMessage(MessageRole.USER, "你好")),
                    List.of(new SystemReminder("仅本轮生效")));

            var response = client(server).streamChat(request, (LlmEventListener) events::add);
            JsonNode body = new ObjectMapper().readTree(server.takeRequest().body());

            ThinkingPart thinking = assertInstanceOf(ThinkingPart.class, response.message().parts().get(0));
            AnthropicThinkingMetadata metadata =
                    assertInstanceOf(AnthropicThinkingMetadata.class, thinking.metadata());
            assertEquals("分析", thinking.text());
            assertEquals("sig-secret", metadata.signature());
            assertEquals("答案", response.text());
            assertEquals(12, response.usage().inputTokens().orElseThrow());
            assertEquals(7, response.usage().outputTokens().orElseThrow());
            assertEquals(2, response.usage().reasoningTokens().orElseThrow());
            assertEquals(3, response.usage().cacheReadTokens().orElseThrow());
            assertEquals(4, response.usage().cacheWriteTokens().orElseThrow());
            assertTrue(events.stream().anyMatch(LlmEvent.ThinkingDelta.class::isInstance));
            assertTrue(events.stream().anyMatch(LlmEvent.ThinkingCompleted.class::isInstance));
            assertInstanceOf(LlmEvent.StreamCompleted.class, events.getLast());
            assertEquals("adaptive", body.path("thinking").path("type").asText());
            assertEquals("high", body.path("output_config").path("effort").asText());
            assertTrue(body.path("system").get(0).path("text").asText()
                    .startsWith("## 身份"));
            assertEquals("ephemeral",
                    body.path("system").get(0).path("cache_control").path("type").asText());
            assertTrue(body.path("messages").get(0).path("content").get(0)
                    .path("text").asText().contains("仅本轮生效"));
        }
    }

    @Test
    void exposesRetryAfterOnRateLimit() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueue(
                    429,
                    "application/json",
                    List.of("{\"error\":{\"type\":\"rate_limit_error\"}}"),
                    0,
                    Map.of("Retry-After", "15"));

            LlmException exception = assertThrows(
                    LlmException.class,
                    () -> client(server).streamChat(
                            new ChatRequest(List.of(new ChatMessage(MessageRole.USER, "你好"))),
                            (LlmEventListener) event -> { }));

            assertEquals(Duration.ofSeconds(15), exception.retryAfter().orElseThrow());
        }
    }

    @Test
    void restoresSignedAndRedactedThinkingHistory() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            server.enqueueSse("""
                    event: content_block_delta
                    data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"完成"}}

                    event: message_stop
                    data: {"type":"message_stop"}

                    """);
            ChatMessage assistant = new ChatMessage(
                    MessageRole.ASSISTANT,
                    List.of(
                            new ThinkingPart(
                                    "分析",
                                    new AnthropicThinkingMetadata("sig-history", "")),
                            new ThinkingPart(
                                    "",
                                    new AnthropicThinkingMetadata("", "redacted-history")),
                            new TextPart("旧回答")));
            ChatRequest request = new ChatRequest(List.of(
                    new ChatMessage(MessageRole.USER, "第一问"),
                    assistant,
                    new ChatMessage(MessageRole.USER, "继续")));

            client(server).streamChat(request, (LlmEventListener) event -> { });
            JsonNode body = new ObjectMapper().readTree(server.takeRequest().body());
            JsonNode content = body.path("messages").get(1).path("content");

            assertEquals("thinking", content.get(0).path("type").asText());
            assertEquals("分析", content.get(0).path("thinking").asText());
            assertEquals("sig-history", content.get(0).path("signature").asText());
            assertEquals("redacted_thinking", content.get(1).path("type").asText());
            assertEquals("redacted-history", content.get(1).path("data").asText());
            assertEquals("text", content.get(2).path("type").asText());
        }
    }

    private static AnthropicClient client(MockLlmServer server) {
        ThinkingConfig thinking = new ThinkingConfig(
                true, ThinkingMode.AUTO, 1024, ReasoningEffort.HIGH, ReasoningSummary.AUTO);
        AppConfig config = new AppConfig(
                Provider.ANTHROPIC,
                "claude-sonnet-4-6",
                "test-key",
                server.baseUri(),
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                4096,
                thinking);
        return new AnthropicClient(
                config,
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                new SseEventReader(),
                new HttpErrorMapper(),
                new ToolRegistry());
    }
}
