package io.imiocode.conversation;

import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConversationSession implements AutoCloseable {
    private final List<ChatMessage> history = new ArrayList<>();
    private final LlmClient client;

    public ConversationSession(LlmClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public ChatResponse send(String userInput, StreamListener listener) throws LlmException {
        Objects.requireNonNull(listener, "listener");
        ChatMessage userMessage = new ChatMessage(MessageRole.USER, userInput);
        List<ChatMessage> requestMessages;
        synchronized (history) {
            requestMessages = new ArrayList<>(history);
        }
        requestMessages.add(userMessage);

        ChatResponse response = client.streamChat(new ChatRequest(requestMessages), listener);
        ChatMessage assistantMessage = new ChatMessage(MessageRole.ASSISTANT, response.content());
        synchronized (history) {
            history.add(userMessage);
            history.add(assistantMessage);
        }
        return response;
    }

    public List<ChatMessage> historySnapshot() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
