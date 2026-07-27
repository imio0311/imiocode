package io.imiocode.agent;

import io.imiocode.config.AgentConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.LlmException;
import io.imiocode.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCancellationTest {
    @Test
    void userCancellationStopsActiveModelRequest() throws Exception {
        BlockingThenSuccessClient client = new BlockingThenSuccessClient();
        Agent agent = new Agent(
                client,
                new ToolRegistry(),
                new AgentConfig(3, Duration.ofSeconds(5), 2)
        );

        try (agent; var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = threads.submit(
                    () -> agent.run(request("阻塞"), AgentEventListener.NOOP));
            assertTrue(client.started.await(2, TimeUnit.SECONDS));
            agent.cancelActive();
            AgentResult result = future.get(2, TimeUnit.SECONDS);

            assertEquals(AgentStopReason.CANCELLED, result.stopReason());
            assertTrue(client.cancelled.get());
        }
    }

    @Test
    void timeoutDoesNotPreventNextTask() {
        BlockingThenSuccessClient client = new BlockingThenSuccessClient();
        AgentResult timedOut;
        AgentResult next;
        try (Agent agent = new Agent(
                client,
                new ToolRegistry(),
                new AgentConfig(3, Duration.ofMillis(80), 2)
        )) {
            timedOut = agent.run(request("等待超时"), AgentEventListener.NOOP);
            next = agent.run(request("下一任务"), AgentEventListener.NOOP);
        }

        assertEquals(AgentStopReason.TIMEOUT, timedOut.stopReason());
        assertTrue(next.completed());
        assertEquals("恢复成功", next.finalResponse().orElseThrow().text());
    }

    private static AgentRequest request(String text) {
        return new AgentRequest(new ChatMessage(MessageRole.USER, text));
    }

    private static final class BlockingThenSuccessClient implements LlmClient {
        private final AtomicInteger calls = new AtomicInteger();
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicBoolean cancelled = new AtomicBoolean();

        @Override
        public ChatResponse streamChat(
                ChatRequest request,
                LlmEventListener listener
        ) throws LlmException {
            if (calls.getAndIncrement() == 0) {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                throw new LlmException(
                        LlmErrorType.INTERRUPTED,
                        true,
                        null,
                        "请求已取消"
                );
            }
            ChatResponse response = new ChatResponse("恢复成功");
            listener.onEvent(new LlmEvent.TextDelta(response.text()));
            listener.onEvent(new LlmEvent.StreamCompleted(response.usage()));
            return response;
        }

        @Override
        public void cancelActiveRequest() {
            cancelled.set(true);
            release.countDown();
        }

        @Override
        public void close() {
            release.countDown();
        }
    }
}
