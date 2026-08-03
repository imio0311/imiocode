package io.imiocode.memory;

import io.imiocode.config.MemoryConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.StreamListener;
import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmMemoryExtractorTest {
    @Test
    void defaultDoesNotResendConversation() {
        FakeClient client = new FakeClient();
        var extractor = new LlmMemoryExtractor(client, MemoryConfig.defaults(),
                new MemoryResponseParser(), new SecretRedactor("secret"));
        var result = extractor.extract(new ChatMessage(MessageRole.USER, "偏好中文"),
                List.of(new ChatMessage(MessageRole.ASSISTANT, "好的")), List.of());
        assertEquals(0, client.calls);
        assertTrue(result.candidates().isEmpty());
    }

    @Test
    void explicitOptInUsesNoToolsAndOnlyText() {
        FakeClient client = new FakeClient();
        MemoryConfig config = new MemoryConfig(true, true, true, true, 200, 1000, 262144, 512);
        var extractor = new LlmMemoryExtractor(client, config, new MemoryResponseParser(), new SecretRedactor("secret"));
        var result = extractor.extract(new ChatMessage(MessageRole.USER, "偏好中文"),
                List.of(new ChatMessage(MessageRole.ASSISTANT, "好的")), List.of());
        assertEquals(1, result.candidates().size());
        assertFalse(client.request.toolSelection().unrestricted());
        assertTrue(client.request.toolSelection().allowedNames().isEmpty());
        assertEquals(512, client.request.outputTokenLimit().orElseThrow());
    }

    private static final class FakeClient implements LlmClient {
        private int calls;
        private ChatRequest request;
        @Override public ChatResponse streamChat(ChatRequest request, StreamListener listener) {
            this.request = request; calls++;
            return new ChatResponse("<memories>{\"items\":[{\"scope\":\"user\",\"category\":\"preference\",\"content\":\"使用中文\",\"replaces_id\":null}]}</memories>");
        }
        @Override public void close() { }
    }
}
