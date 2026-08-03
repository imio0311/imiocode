package io.imiocode.conversation;

import io.imiocode.agent.Agent;
import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentRequest;
import io.imiocode.agent.AgentResult;
import io.imiocode.config.AgentConfig;
import io.imiocode.context.CompactReport;
import io.imiocode.context.ContextResult;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.permission.PermissionReply;
import io.imiocode.tool.ToolExecutor;
import io.imiocode.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 会话历史的事务边界：只有 Agent 正常完成时才提交完整临时轨迹。
 */
public final class ConversationSession implements AutoCloseable {
    private final List<ChatMessage> history = new ArrayList<>();
    private final List<SystemReminder> pendingReminders = new ArrayList<>();
    private final Agent agent;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean active = new AtomicBoolean();

    public ConversationSession(Agent agent) {
        this.agent = Objects.requireNonNull(agent, "agent");
    }

    public ConversationSession(LlmClient client) {
        this(new Agent(client, new ToolRegistry(), AgentConfig.defaults()));
    }

    public ConversationSession(LlmClient client, ToolExecutor executor) {
        this(new Agent(client, executor.registry(), AgentConfig.defaults()));
    }

    public ChatResponse send(String userInput, StreamListener listener) throws LlmException {
        Objects.requireNonNull(listener, "listener");
        try {
            return sendWithEvents(userInput, listener::onTextDelta);
        } catch (ConversationException exception) {
            throw new LlmException(
                    exception.interrupted() ? LlmErrorType.INTERRUPTED : LlmErrorType.PROTOCOL,
                    exception.recoverable(),
                    null,
                    exception.safeMessage(),
                    exception.retryAfter().orElse(null),
                    exception
            );
        }
    }

    public synchronized ChatResponse sendWithEvents(
            String userInput,
            ConversationListener listener
    ) throws ConversationException {
        Objects.requireNonNull(listener, "listener");
        if (closed.get()) {
            throw new ConversationException("会话已关闭", false, true, false);
        }
        if (!active.compareAndSet(false, true)) {
            throw new ConversationException("会话正在执行任务", true, false, false);
        }
        try {
            ChatMessage userMessage = new ChatMessage(MessageRole.USER, userInput);
            List<SystemReminder> reminders;
            synchronized (pendingReminders) {
                reminders = List.copyOf(pendingReminders);
                pendingReminders.clear();
            }
            List<ChatMessage> committed;
            synchronized (history) {
                committed = List.copyOf(history);
            }

            AgentResult result = agent.run(
                    new AgentRequest(committed, userMessage, reminders),
                    listener::onAgentEvent
            );
            applyResultHistory(result);
            if (!result.completed()) {
                throw ConversationException.from(result);
            }
            if (closed.get()) {
                throw new ConversationException("会话已关闭", false, true, result.toolsExecuted());
            }
            return result.finalResponse().orElseThrow();
        } finally {
            active.set(false);
        }
    }

    private void applyResultHistory(AgentResult result) {
        synchronized (history) {
            history.clear();
            history.addAll(result.committedHistory());
            if (result.completed()) {
                history.addAll(result.trajectory());
            }
        }
    }

    public List<ChatMessage> historySnapshot() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }

    public boolean isActive() {
        return active.get();
    }

    /** 仅用于已经通过外部完整性校验的会话恢复。 */
    public void replaceHistory(List<ChatMessage> restoredHistory) {
        Objects.requireNonNull(restoredHistory, "restoredHistory");
        if (closed.get()) throw new IllegalStateException("会话已关闭");
        if (active.get()) throw new IllegalStateException("会话正在执行任务");
        synchronized (history) {
            history.clear();
            history.addAll(List.copyOf(restoredHistory));
        }
        synchronized (pendingReminders) {
            pendingReminders.clear();
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

    public AgentMode mode() {
        return agent.mode();
    }

    public void switchMode(AgentMode mode, ConversationListener listener) {
        Objects.requireNonNull(listener, "listener");
        agent.switchMode(mode, listener::onAgentEvent);
    }

    public void cancelActive() {
        agent.cancelActive();
    }

    public synchronized CompactReport forceCompact(ConversationListener listener) {
        Objects.requireNonNull(listener, "listener");
        List<ChatMessage> snapshot = historySnapshot();
        if (snapshot.isEmpty()) {
            return new CompactReport(0, 0, 0, false,
                    io.imiocode.context.ContextOutcome.UNCHANGED, "当前没有可压缩的历史");
        }
        ContextResult result = agent.forceCompactHistory(snapshot, listener::onAgentEvent);
        if (result.compacted()) {
            synchronized (history) {
                history.clear();
                history.addAll(result.workingMessages());
            }
        }
        return new CompactReport(result.beforeTokens(), result.afterTokens(), result.spilledResults(),
                result.compacted(), result.outcome(), result.compacted() ? "上下文压缩完成" : "上下文未发生变化");
    }

    public boolean respondPermission(String requestId, PermissionReply reply) {
        if (closed.get()) {
            return false;
        }
        return agent.respondPermission(requestId, reply);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            agent.close();
        }
    }
}
