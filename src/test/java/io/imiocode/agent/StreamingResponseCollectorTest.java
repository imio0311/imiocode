package io.imiocode.agent;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingResponseCollectorTest {
    @Test
    void forwardsSafeDeltasAndReturnsCompleteResponse() throws Exception {
        List<AgentEvent> events = new ArrayList<>();
        ChatResponse expected = new ChatResponse("完成");
        LlmClient client = richClient(List.of(
                new LlmEvent.TextDelta("完"),
                new LlmEvent.ThinkingDelta(0, "分析"),
                new LlmEvent.ToolCallStarted(0, "c1", "read_file"),
                new LlmEvent.ToolCallDelta(0, "{\"path\":"),
                new LlmEvent.StreamCompleted(TokenUsage.unknown())
        ), expected);

        ChatResponse actual = new StreamingResponseCollector(client).collect(
                request(), 1, events::add);

        assertEquals(expected, actual);
        assertTrue(events.stream().anyMatch(AgentEvent.TextDelta.class::isInstance));
        assertTrue(events.stream().anyMatch(AgentEvent.ThinkingDelta.class::isInstance));
        assertTrue(events.stream().anyMatch(AgentEvent.ModelToolRequested.class::isInstance));
        assertTrue(events.stream().anyMatch(AgentEvent.ModelResponseCompleted.class::isInstance));
        assertEquals(4, events.size());
    }

    @Test
    void rejectsMissingOrDuplicateCompletion() {
        StreamingResponseCollector missing = new StreamingResponseCollector(
                richClient(List.of(new LlmEvent.TextDelta("x")), new ChatResponse("x")));
        StreamingResponseCollector duplicate = new StreamingResponseCollector(richClient(List.of(
                new LlmEvent.StreamCompleted(TokenUsage.unknown()),
                new LlmEvent.StreamCompleted(TokenUsage.unknown())
        ), new ChatResponse("x")));

        assertThrows(LlmException.class,
                () -> missing.collect(request(), 1, AgentEventListener.NOOP));
        assertThrows(LlmException.class,
                () -> duplicate.collect(request(), 1, AgentEventListener.NOOP));
    }

    private static ChatRequest request() {
        return new ChatRequest(List.of(new ChatMessage(MessageRole.USER, "你好")));
    }

    private static LlmClient richClient(List<LlmEvent> events, ChatResponse response) {
        return new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) {
                events.forEach(listener::onEvent);
                return response;
            }

            @Override
            public void close() {
            }
        };
    }
}
