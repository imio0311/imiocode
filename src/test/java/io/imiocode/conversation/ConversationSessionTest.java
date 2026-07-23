package io.imiocode.conversation;

import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolExecutor;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversationSessionTest {
    @Test
    void carriesSuccessfulHistoryIntoSecondTurn() throws Exception {
        FakeClient client = new FakeClient(List.of("第一轮", "IMIO-2749"));
        ConversationSession session = new ConversationSession(client);

        session.send("记住 IMIO-2749", text -> { });
        session.send("验证码是什么", text -> { });

        assertEquals(2, client.requests.size());
        assertEquals(3, client.requests.get(1).messages().size());
        assertEquals("第一轮", client.requests.get(1).messages().get(1).content());
        assertEquals(4, session.historySnapshot().size());
        assertThrows(UnsupportedOperationException.class,
                () -> session.historySnapshot().add(new ChatMessage(MessageRole.USER, "x")));
    }

    @Test
    void doesNotCommitPartialFailedTurn() throws Exception {
        LlmClient client = new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
                listener.onTextDelta("部分");
                throw new LlmException(LlmErrorType.NETWORK, true, null, "断流");
            }

            @Override
            public void close() {
            }
        };
        ConversationSession session = new ConversationSession(client);

        assertThrows(LlmException.class, () -> session.send("失败消息", text -> { }));
        assertEquals(List.of(), session.historySnapshot());
    }

    @Test
    void executesFirstToolBatchSeriallyAndCommitsWholeRound() throws Exception {
        ToolCall read = call("c1", "read_file");
        ToolCall missing = call("c2", "missing_tool");
        SequencedClient client = new SequencedClient(List.of(
                new ChatResponse(new ChatMessage(MessageRole.ASSISTANT,
                        List.of(new ToolCallPart(read), new ToolCallPart(missing)))),
                new ChatResponse("最终回答")));
        ToolRegistry registry = new ToolRegistry();
        registry.register(stub("read_file"));
        ConversationSession session = new ConversationSession(client, new ToolExecutor(registry));

        ChatResponse response = session.sendWithEvents("读取", text -> { });

        assertEquals("最终回答", response.text());
        assertEquals(2, client.requests.size());
        assertEquals(3, client.requests.get(1).messages().size());
        ChatMessage toolMessage = client.requests.get(1).messages().get(2);
        assertEquals(MessageRole.TOOL, toolMessage.role());
        assertEquals(List.of("c1", "c2"), toolMessage.parts().stream()
                .map(ToolResultPart.class::cast).map(ToolResultPart::callId).toList());
        assertTrue(((ToolResultPart) toolMessage.parts().get(0)).result().success());
        assertFalse(((ToolResultPart) toolMessage.parts().get(1)).result().success());
        assertEquals(4, session.historySnapshot().size());
    }

    @Test
    void refusesSecondToolBatchAndRollsBackHistory() {
        ToolCall call = call("c1", "read_file");
        SequencedClient client = new SequencedClient(List.of(
                toolResponse(call),
                toolResponse(call("c2", "read_file"))));
        ToolRegistry registry = new ToolRegistry();
        registry.register(stub("read_file"));
        ConversationSession session = new ConversationSession(client, new ToolExecutor(registry));

        ConversationException exception = assertThrows(
                ConversationException.class,
                () -> session.sendWithEvents("读取", text -> { }));

        assertTrue(exception.toolsExecuted());
        assertTrue(exception.safeMessage().contains("只执行一批"));
        assertEquals(List.of(), session.historySnapshot());
    }

    @Test
    void finalModelFailureReportsExecutedSideEffectsAndRollsBack() {
        ToolCall call = call("c1", "read_file");
        LlmClient client = new LlmClient() {
            private int calls;

            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
                calls++;
                if (calls == 1) {
                    return toolResponse(call);
                }
                throw new LlmException(LlmErrorType.NETWORK, true, null, "后续请求失败");
            }

            @Override
            public void close() {
            }
        };
        ToolRegistry registry = new ToolRegistry();
        registry.register(stub("read_file"));
        ConversationSession session = new ConversationSession(client, new ToolExecutor(registry));

        ConversationException exception = assertThrows(
                ConversationException.class,
                () -> session.sendWithEvents("读取", text -> { }));

        assertTrue(exception.toolsExecuted());
        assertEquals(List.of(), session.historySnapshot());
    }

    @Test
    void closeIsIdempotentAndPreventsFurtherRequests() {
        AtomicInteger closes = new AtomicInteger();
        LlmClient client = new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener) {
                return new ChatResponse("不应调用");
            }

            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };
        ConversationSession session = new ConversationSession(client);

        session.close();
        session.close();

        assertEquals(1, closes.get());
        assertThrows(ConversationException.class,
                () -> session.sendWithEvents("消息", text -> { }));
    }

    @Test
    void closeCancelsActiveToolAndDoesNotSendFollowUp() throws Exception {
        ToolCall call = call("c1", "blocking");
        AtomicInteger clientCalls = new AtomicInteger();
        LlmClient client = new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener) {
                clientCalls.incrementAndGet();
                return toolResponse(call);
            }

            @Override
            public void close() {
            }
        };
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean cancelled = new AtomicBoolean();
        Tool blocking = new Tool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(
                        "blocking", "阻塞工具",
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                                .put("type", "object"),
                        ToolRisk.HIGH);
            }

            @Override
            public ToolResult execute(com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return ToolResult.interrupted("", "已取消", false);
            }

            @Override
            public void cancel() {
                cancelled.set(true);
                release.countDown();
            }
        };
        ToolRegistry registry = new ToolRegistry();
        registry.register(blocking);
        ConversationSession session = new ConversationSession(client, new ToolExecutor(registry));

        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = threads.submit(() -> session.sendWithEvents("执行", text -> { }));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            session.close();
            ExecutionException exception = assertThrows(
                    ExecutionException.class,
                    () -> future.get(2, TimeUnit.SECONDS));
            assertTrue(exception.getCause() instanceof ConversationException);
            assertTrue(((ConversationException) exception.getCause()).interrupted());
            assertTrue(cancelled.get());
            assertEquals(1, clientCalls.get());
            assertEquals(List.of(), session.historySnapshot());
        }
    }

    private static ChatResponse toolResponse(ToolCall call) {
        return new ChatResponse(new ChatMessage(
                MessageRole.ASSISTANT, List.of(new ToolCallPart(call))));
    }

    private static ToolCall call(String id, String name) {
        return new ToolCall(id, name,
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode());
    }

    private static Tool stub(String name) {
        return new Tool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(
                        name,
                        "测试工具",
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                                .put("type", "object"),
                        ToolRisk.LOW);
            }

            @Override
            public ToolResult execute(com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                return ToolResult.success("ok");
            }
        };
    }

    private static final class FakeClient implements LlmClient {
        private final List<String> responses;
        private final List<ChatRequest> requests = new ArrayList<>();

        private FakeClient(List<String> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse streamChat(ChatRequest request, StreamListener listener) {
            requests.add(request);
            String response = responses.get(requests.size() - 1);
            listener.onTextDelta(response);
            return new ChatResponse(response);
        }

        @Override
        public void close() {
        }
    }

    private static final class SequencedClient implements LlmClient {
        private final List<ChatResponse> responses;
        private final List<ChatRequest> requests = new ArrayList<>();

        private SequencedClient(List<ChatResponse> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse streamChat(ChatRequest request, StreamListener listener) {
            requests.add(request);
            ChatResponse response = responses.get(requests.size() - 1);
            if (!response.text().isEmpty()) {
                listener.onTextDelta(response.text());
            }
            return response;
        }

        @Override
        public void close() {
        }
    }
}
