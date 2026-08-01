package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmEventListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationSummarizerTest {
    @Test
    void sendsSingleSilentNoToolRequestWithOverride() throws Exception {
        CapturingClient client = new CapturingClient();
        ConversationSummarizer summarizer = new ConversationSummarizer(
                client, new ConversationSerializer(), new SummaryParser());

        ParsedSummary result = summarizer.summarize(
                List.of(new ChatMessage(MessageRole.USER, "旧目标")),
                List.of(new ChatMessage(MessageRole.USER, "当前任务")));

        assertEquals("旧摘要", result.priorHistory());
        assertEquals(1, client.calls);
        assertTrue(client.request.toolSelection().allowedNames().isEmpty());
        assertTrue(client.request.systemPromptOverride().orElseThrow().contains("上下文压缩器"));
        assertTrue(client.request.reminders().isEmpty());
    }

    private static final class CapturingClient implements LlmClient {
        private int calls;
        private ChatRequest request;

        @Override
        public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) {
            calls++;
            this.request = request;
            return new ChatResponse("<summary><prior_history>旧摘要</prior_history><active_task>当前摘要</active_task></summary>");
        }

        @Override public void close() { }
    }
}
