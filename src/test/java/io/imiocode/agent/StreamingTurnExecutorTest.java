package io.imiocode.agent;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.TokenUsage;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamingTurnExecutorTest {
    @Test
    void retriesRecoverableFailureBeforeAnyToolStarts() throws Exception {
        ScriptedClient client = new ScriptedClient(LlmErrorType.NETWORK);
        List<AgentEvent> events = new ArrayList<>();
        StreamingTurnExecutor executor = executor(client);

        StreamingTurnResult result = executor.execute(
                request(8_000),
                1,
                true,
                new AgentTaskContext(Duration.ofSeconds(10)),
                new UnknownToolCircuitBreaker(),
                events::add);

        assertEquals("完成", result.response().text());
        assertEquals(2, client.calls.get());
        assertEquals(1, events.stream()
                .filter(AgentEvent.RetryScheduled.class::isInstance).count());
        assertTrue(result.toolExecutions().isEmpty());
    }

    @Test
    void outputLimitDoublesOnlyCurrentAttempt() throws Exception {
        ScriptedClient client = new ScriptedClient(LlmErrorType.OUTPUT_LIMIT);
        StreamingTurnResult result = executor(client).execute(
                request(8_000),
                1,
                true,
                new AgentTaskContext(Duration.ofSeconds(10)),
                new UnknownToolCircuitBreaker(),
                AgentEventListener.NOOP);

        assertEquals("完成", result.response().text());
        assertEquals(List.of(8_000, 16_000), client.outputLimits);
    }

    @Test
    void retryPreservesSystemPromptOverride() throws Exception {
        ScriptedClient client = new ScriptedClient(LlmErrorType.NETWORK);
        ChatRequest request = new ChatRequest(
                List.of(new ChatMessage(MessageRole.USER, "摘要数据")), List.of(),
                io.imiocode.tool.ToolSelection.only(java.util.Set.of()), OptionalInt.of(1_000),
                Optional.of("摘要专用提示"));

        executor(client).execute(request, 1, false, new AgentTaskContext(Duration.ofSeconds(10)),
                new UnknownToolCircuitBreaker(), AgentEventListener.NOOP);

        assertEquals(List.of("摘要专用提示", "摘要专用提示"), client.overrides);
    }

    @Test
    void doesNotRetryAfterLowToolStarts() {
        AtomicInteger calls = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry();
        registry.register(new Tool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(
                        "read", "read",
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
                        ToolRisk.LOW);
            }

            @Override
            public ToolResult execute(
                    com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                return ToolResult.success("ok");
            }
        });
        LlmClient client = new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, LlmEventListener listener)
                    throws LlmException {
                calls.incrementAndGet();
                ToolCall call = new ToolCall(
                        "c1", "read",
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode());
                listener.onEvent(new LlmEvent.ToolCallStarted(0, call.id(), call.name()));
                listener.onEvent(new LlmEvent.ToolCallCompleted(0, call));
                throw new LlmException(LlmErrorType.NETWORK, true, null, "断流");
            }

            @Override
            public void close() {
            }
        };
        AgentTaskContext task = new AgentTaskContext(Duration.ofSeconds(10));
        StreamingTurnExecutor executor = new StreamingTurnExecutor(
                client, registry, 1, new LlmRetryPolicy(), (delay, context) -> {
            throw new AssertionError("工具开始后不应等待重试");
        });

        assertThrows(LlmException.class, () -> executor.execute(
                request(8_000), 1, true, task,
                new UnknownToolCircuitBreaker(), AgentEventListener.NOOP));
        assertEquals(1, calls.get());
        assertTrue(task.toolsExecuted());
    }

    private static StreamingTurnExecutor executor(ScriptedClient client) {
        return new StreamingTurnExecutor(
                client,
                new ToolRegistry(),
                2,
                new LlmRetryPolicy(),
                (delay, task) -> {
                });
    }

    private static ChatRequest request(int limit) {
        return new ChatRequest(
                List.of(new ChatMessage(MessageRole.USER, "你好")),
                List.of(),
                io.imiocode.tool.ToolSelection.allEnabled(),
                OptionalInt.of(limit));
    }

    private static final class ScriptedClient implements LlmClient {
        private final LlmErrorType firstFailure;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<Integer> outputLimits = new ArrayList<>();
        private final List<String> overrides = new ArrayList<>();

        private ScriptedClient(LlmErrorType firstFailure) {
            this.firstFailure = firstFailure;
        }

        @Override
        public ChatResponse streamChat(ChatRequest request, LlmEventListener listener)
                throws LlmException {
            int call = calls.incrementAndGet();
            outputLimits.add(request.outputTokenLimit().orElseThrow());
            overrides.add(request.systemPromptOverride().orElse(""));
            if (call == 1) {
                listener.onEvent(new LlmEvent.TextDelta("半截"));
                throw new LlmException(firstFailure, true, null, "可恢复失败");
            }
            listener.onEvent(new LlmEvent.TextDelta("完成"));
            listener.onEvent(new LlmEvent.StreamCompleted(TokenUsage.unknown()));
            return new ChatResponse("完成");
        }

        @Override
        public void close() {
        }
    }
}
