package io.imiocode.llm;

import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;

public interface LlmClient extends AutoCloseable {
    ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException;

    @Override
    void close();
}
