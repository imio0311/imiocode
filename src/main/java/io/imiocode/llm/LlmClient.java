package io.imiocode.llm;

import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;

public interface LlmClient extends AutoCloseable {
    default ChatResponse streamChat(ChatRequest request, LlmEventListener listener) throws LlmException {
        StreamListener textListener = text -> listener.onEvent(new LlmEvent.TextDelta(text));
        ChatResponse response = streamChat(request, textListener);
        listener.onEvent(new LlmEvent.StreamCompleted(response.usage()));
        return response;
    }

    default ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
        return streamChat(request, (LlmEventListener) listener);
    }

    default void cancelActiveRequest() {
    }

    @Override
    void close();
}
