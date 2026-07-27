package io.imiocode.llm;

import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;

public interface LlmClient extends AutoCloseable {
    default ChatResponse streamChat(ChatRequest request, LlmEventListener listener) throws LlmException {
        StreamListener textListener = text -> listener.onEvent(new LlmEvent.TextDelta(text));
        return streamChat(request, textListener);
    }

    default ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
        return streamChat(request, (LlmEventListener) listener);
    }

    @Override
    void close();
}
