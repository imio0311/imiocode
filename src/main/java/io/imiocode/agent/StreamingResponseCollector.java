package io.imiocode.agent;

import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.LlmException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 收集 Provider 的完整流式响应，同时把安全的增量事件实时透传给 Agent UI。
 */
public final class StreamingResponseCollector {
    private final LlmClient client;

    public StreamingResponseCollector(LlmClient client) {
        this.client = Objects.requireNonNull(client, "client 不能为空");
    }

    public ChatResponse collect(
            ChatRequest request,
            int iteration,
            AgentEventListener listener
    ) throws LlmException {
        return collect(request, iteration, 1, listener, (index, call) -> {
        });
    }

    public ChatResponse collect(
            ChatRequest request,
            int iteration,
            int attempt,
            AgentEventListener listener,
            ToolCallCompletionListener toolCalls
    ) throws LlmException {
        Objects.requireNonNull(request, "request 不能为空");
        if (iteration <= 0) {
            throw new IllegalArgumentException("iteration 必须大于 0");
        }
        if (attempt <= 0) {
            throw new IllegalArgumentException("attempt 必须大于 0");
        }
        ToolCallCompletionListener checkedToolCalls =
                Objects.requireNonNull(toolCalls, "toolCalls 不能为空");
        AgentEventListener checkedListener =
                Objects.requireNonNullElse(listener, AgentEventListener.NOOP);
        AtomicInteger completionCount = new AtomicInteger();

        ChatResponse response = client.streamChat(request, (LlmEventListener) event -> {
            if (event instanceof LlmEvent.TextDelta textDelta) {
                checkedListener.onEvent(new AgentEvent.TextDelta(iteration, textDelta.text()));
            } else if (event instanceof LlmEvent.ThinkingDelta thinkingDelta) {
                checkedListener.onEvent(
                        new AgentEvent.ThinkingDelta(iteration, thinkingDelta.text()));
            } else if (event instanceof LlmEvent.ThinkingCompleted) {
                checkedListener.onEvent(new AgentEvent.ThinkingCompleted(iteration));
            } else if (event instanceof LlmEvent.ToolCallStarted started) {
                checkedListener.onEvent(new AgentEvent.ModelToolRequested(
                        iteration,
                        started.index(),
                        started.id(),
                        started.name()
                ));
            } else if (event instanceof LlmEvent.ToolCallCompleted completed) {
                checkedToolCalls.onCompleted(completed.index(), completed.call());
            } else if (event instanceof LlmEvent.StreamCompleted) {
                completionCount.incrementAndGet();
            }
        });

        if (completionCount.get() != 1) {
            throw new LlmException(
                    LlmErrorType.PROTOCOL,
                    false,
                    null,
                    "模型流必须且只能包含一个完成事件"
            );
        }
        checkedListener.onEvent(new AgentEvent.ModelResponseCompleted(
                iteration,
                response.usage(),
                response.hasToolCalls()
        ));
        return response;
    }

    @FunctionalInterface
    public interface ToolCallCompletionListener {
        void onCompleted(int originalIndex, io.imiocode.tool.ToolCall call);
    }
}
