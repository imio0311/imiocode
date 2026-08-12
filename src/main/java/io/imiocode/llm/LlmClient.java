package io.imiocode.llm;

import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;

/**
 * LLM Provider 的统一流式调用边界。
 *
 * <p>实现方可以覆盖结构化事件或纯文本监听器中的任一入口；默认方法负责在两种监听模型之间桥接，
 * 上层 Agent 因而不需要了解 Provider 的具体流式协议。</p>
 */
public interface LlmClient extends AutoCloseable {
    /**
     * 流式执行一次对话，并向监听器发送文本、推理、用量等结构化事件。
     *
     * @param request 完整对话请求
     * @param listener 结构化流事件监听器
     * @return 流结束后组装的完整响应
     * @throws LlmException 请求、协议解析或 Provider 响应失败时抛出
     */
    default ChatResponse streamChat(ChatRequest request, LlmEventListener listener) throws LlmException {
        StreamListener textListener = text -> listener.onEvent(new LlmEvent.TextDelta(text));
        ChatResponse response = streamChat(request, textListener);
        listener.onEvent(new LlmEvent.StreamCompleted(response.usage()));
        return response;
    }

    default ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
        return streamChat(request, (LlmEventListener) listener);
    }

    /** 请求尽力取消当前活动调用；没有活动请求或 Provider 不支持取消时允许无操作。 */
    default void cancelActiveRequest() {
    }

    @Override
    void close();
}
