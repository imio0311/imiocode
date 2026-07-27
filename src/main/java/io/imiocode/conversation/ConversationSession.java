package io.imiocode.conversation;

import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.tool.ToolExecution;
import io.imiocode.tool.ToolExecutor;
import io.imiocode.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConversationSession implements AutoCloseable {
    private final List<ChatMessage> history = new ArrayList<>();
    private final List<SystemReminder> pendingReminders = new ArrayList<>();
    private final LlmClient client;
    private final ToolExecutor executor;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ConversationSession(LlmClient client) {
        this(client, new ToolExecutor(new ToolRegistry()));
    }

    public ConversationSession(LlmClient client, ToolExecutor executor) {
        this.client = Objects.requireNonNull(client, "client");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public ChatResponse send(String userInput, StreamListener listener) throws LlmException {
        Objects.requireNonNull(listener, "listener");
        try {
            return sendWithEvents(userInput, new ConversationListener() {
                @Override
                public void onTextDelta(String text) {
                    listener.onTextDelta(text);
                }
            });
        } catch (ConversationException exception) {
            if (exception.getCause() instanceof LlmException llmException) {
                throw llmException;
            }
            throw new LlmException(
                    exception.interrupted() ? LlmErrorType.INTERRUPTED : LlmErrorType.PROTOCOL,
                    exception.recoverable(),
                    null,
                    exception.safeMessage(),
                    exception);
        }
    }

    public synchronized ChatResponse sendWithEvents(
            String userInput,
            ConversationListener listener) throws ConversationException {
        Objects.requireNonNull(listener, "listener");
        if (closed.get()) {
            throw new ConversationException("会话已关闭", false, true, false);
        }
        ChatMessage userMessage = new ChatMessage(MessageRole.USER, userInput);
        List<SystemReminder> reminders;
        synchronized (pendingReminders) {
            reminders = List.copyOf(pendingReminders);
            pendingReminders.clear();
        }
        List<ChatMessage> requestMessages;
        synchronized (history) {
            requestMessages = new ArrayList<>(history);
        }
        requestMessages.add(userMessage);

        ChatResponse first;
        try {
            first = request(new ChatRequest(requestMessages, reminders), listener);
        } catch (LlmException exception) {
            throw ConversationException.from(exception, false);
        }
        if (!first.hasToolCalls()) {
            if (closed.get()) {
                throw new ConversationException("会话已关闭", false, true, false);
            }
            commit(List.of(userMessage, first.message()));
            return first;
        }

        List<ToolExecution> executions = executor.executeAll(first.toolCalls(), listener::onToolEvent);
        boolean toolsExecuted = !executions.isEmpty();
        if (executions.size() != first.toolCalls().size() || closed.get()) {
            throw new ConversationException("工具执行已中断", false, true, toolsExecuted);
        }
        List<MessagePart> resultParts = executions.stream()
                .map(execution -> new ToolResultPart(
                        execution.call().id(),
                        execution.call().name(),
                        execution.result()))
                .map(MessagePart.class::cast)
                .toList();
        ChatMessage toolMessage = new ChatMessage(MessageRole.TOOL, resultParts);
        List<ChatMessage> followUp = new ArrayList<>(requestMessages);
        followUp.add(first.message());
        followUp.add(toolMessage);

        ChatResponse finalResponse;
        try {
            finalResponse = request(new ChatRequest(followUp, reminders), listener);
        } catch (LlmException exception) {
            throw ConversationException.from(exception, true);
        }
        if (finalResponse.hasToolCalls()) {
            throw new ConversationException(
                    "本章每轮只执行一批工具，模型再次请求的工具未执行",
                    true,
                    false,
                    true);
        }
        if (closed.get()) {
            throw new ConversationException("会话已关闭", false, true, true);
        }
        commit(List.of(userMessage, first.message(), toolMessage, finalResponse.message()));
        return finalResponse;
    }

    private ChatResponse request(ChatRequest request, ConversationListener listener) throws LlmException {
        listener.onResponseStarted();
        ChatResponse response = client.streamChat(request, listener::onLlmEvent);
        listener.onResponseCompleted();
        return response;
    }

    private void commit(List<ChatMessage> messages) {
        synchronized (history) {
            history.addAll(messages);
        }
    }

    public List<ChatMessage> historySnapshot() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    public void addSystemReminder(String content) {
        SystemReminder reminder = new SystemReminder(content);
        synchronized (pendingReminders) {
            if (closed.get()) {
                throw new IllegalStateException("会话已关闭");
            }
            pendingReminders.add(reminder);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            executor.close();
            client.close();
        }
    }
}
