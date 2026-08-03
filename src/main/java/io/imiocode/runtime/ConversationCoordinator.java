package io.imiocode.runtime;

import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentEvent;
import io.imiocode.command.CommandServices;
import io.imiocode.config.MemoryConfig;
import io.imiocode.config.SessionsConfig;
import io.imiocode.context.CompactReport;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.ConversationException;
import io.imiocode.conversation.ConversationListener;
import io.imiocode.conversation.ConversationSession;
import io.imiocode.memory.MemoryDocument;
import io.imiocode.memory.MemoryEntry;
import io.imiocode.memory.MemoryExtractor;
import io.imiocode.memory.MemoryManager;
import io.imiocode.memory.MemoryScope;
import io.imiocode.persistence.PersistenceEvent;
import io.imiocode.persistence.PersistenceEventListener;
import io.imiocode.persistence.PersistentContextProvider;
import io.imiocode.permission.PermissionReply;
import io.imiocode.session.SessionId;
import io.imiocode.session.SessionLoadResult;
import io.imiocode.session.SessionManager;
import io.imiocode.session.SessionMetadata;
import io.imiocode.session.SessionSnapshot;
import io.imiocode.session.SessionSummary;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/** 在核心会话之外编排持久化和记忆，保持底层 Agent 依赖单向。 */
public final class ConversationCoordinator implements CommandServices, AutoCloseable {
    private final ConversationSession core;
    private final SessionManager sessions;
    private final SessionsConfig sessionsConfig;
    private final MemoryManager memories;
    private final MemoryConfig memoryConfig;
    private final MemoryExtractor extractor;
    private final PersistentContextProvider context;
    private final PersistenceEventListener events;
    private final Clock clock;
    private SessionSnapshot current;

    public ConversationCoordinator(
            ConversationSession core,
            SessionManager sessions,
            SessionsConfig sessionsConfig,
            MemoryManager memories,
            MemoryConfig memoryConfig,
            MemoryExtractor extractor,
            PersistentContextProvider context,
            PersistenceEventListener events,
            Clock clock) {
        this.core = core; this.sessions = sessions; this.sessionsConfig = sessionsConfig;
        this.memories = memories; this.memoryConfig = memoryConfig; this.extractor = extractor;
        this.context = context; this.events = events == null ? PersistenceEventListener.noop() : events;
        this.clock = clock;
        this.current = sessionsConfig.enabled() ? sessions.createNew() : ephemeral(List.of());
        if (sessionsConfig.enabled()) sessions.applyRetention(current.metadata().id());
    }

    public ChatResponse sendWithEvents(String userInput, ConversationListener listener) throws ConversationException {
        ChatMessage userMessage = new ChatMessage(io.imiocode.conversation.MessageRole.USER, userInput);
        context.currentReminders().forEach(reminder -> core.addSystemReminder(reminder.content()));
        ChatResponse response = core.sendWithEvents(userInput, listener);
        List<ChatMessage> afterHistory = core.historySnapshot();

        boolean persisted = !sessionsConfig.enabled();
        if (sessionsConfig.enabled()) {
            try {
                current = sessions.commit(current, afterHistory);
                persisted = true;
                events.onEvent(new PersistenceEvent.SessionSaved(
                        current.metadata().id(), current.metadata().commitCount()));
            } catch (RuntimeException exception) {
                events.onEvent(new PersistenceEvent.Warning("[会话] 本轮未能落盘，内存回复仍然有效"));
            }
        } else {
            current = ephemeral(afterHistory);
        }

        if (persisted && memoryConfig.enabled() && memoryConfig.autoExtract()) {
            var extraction = extractor.extract(userMessage, afterHistory, memories.loadEnabledScopes());
            extraction.warnings().forEach(warning -> events.onEvent(new PersistenceEvent.Warning("[记忆] " + warning)));
            if (!extraction.candidates().isEmpty()) {
                try {
                    var report = memories.applyCandidates(extraction.candidates());
                    context.reloadMemories();
                    events.onEvent(new PersistenceEvent.MemoryUpdated(report.added(), report.updated(), report.skipped()));
                    report.warnings().forEach(warning -> events.onEvent(new PersistenceEvent.Warning("[记忆] " + warning)));
                } catch (RuntimeException exception) {
                    events.onEvent(new PersistenceEvent.Warning("[记忆] 自动更新失败，已保留原文件"));
                }
            }
        }
        return response;
    }

    @Override public AgentMode mode() { return core.mode(); }

    @Override
    public void switchMode(AgentMode mode) {
        core.switchMode(mode, noopConversationListener());
    }

    @Override
    public CompactReport compact() {
        List<ChatMessage> before = core.historySnapshot();
        CompactReport report = core.forceCompact(noopConversationListener());
        if (report.compacted()) {
            List<ChatMessage> after = core.historySnapshot();
            if (sessionsConfig.enabled()) {
                try { current = sessions.commit(current, after); }
                catch (RuntimeException exception) {
                    events.onEvent(new PersistenceEvent.Warning("[会话] 压缩结果未能落盘"));
                }
            } else current = ephemeral(after);
        }
        return report;
    }

    @Override public SessionSummary currentSession() { return summary(current); }
    @Override public List<SessionSummary> listSessions() { return sessionsConfig.enabled() ? sessions.list() : List.of(summary(current)); }

    @Override
    public SessionSummary newSession() {
        requireIdle();
        SessionSnapshot next = sessionsConfig.enabled() ? sessions.createNew() : newEphemeral();
        core.replaceHistory(List.of()); current = next;
        context.reloadInstructions(); context.reloadMemories();
        return summary(next);
    }

    @Override
    public SessionLoadResult resumeSession(SessionId id) {
        requireSessions(); requireIdle();
        SessionLoadResult loaded = sessions.load(id);
        core.replaceHistory(loaded.snapshot().history()); current = loaded.snapshot();
        context.reloadInstructions(); context.reloadMemories();
        events.onEvent(new PersistenceEvent.SessionRestored(id, loaded.snapshot().history().size()));
        loaded.quarantinedTail().ifPresent(path -> events.onEvent(new PersistenceEvent.SessionTailRecovered(id, path)));
        return loaded;
    }

    @Override public void deleteSession(SessionId id) { requireSessions(); if (id.equals(current.metadata().id())) throw new IllegalStateException("不能删除当前会话"); sessions.delete(id); }
    @Override public boolean sessionsEnabled() { return sessionsConfig.enabled(); }

    @Override
    public List<MemoryDocument> listMemories(Optional<MemoryScope> scope) {
        List<MemoryDocument> all = memories.loadEnabledScopes();
        return scope.map(value -> all.stream().filter(item -> item.scope() == value).toList()).orElse(all);
    }
    @Override public MemoryEntry addMemory(MemoryScope scope, String content) { MemoryEntry entry = memories.add(scope, content); context.reloadMemories(); return entry; }
    @Override public MemoryEntry editMemory(MemoryScope scope, String id, String content) { MemoryEntry entry = memories.edit(scope, id, content); context.reloadMemories(); return entry; }
    @Override public void forgetMemory(MemoryScope scope, String id) { memories.forget(scope, id); context.reloadMemories(); }
    @Override public boolean memoryEnabled() { return memories.enabled(); }

    public boolean respondPermission(String requestId, PermissionReply reply) { return core.respondPermission(requestId, reply); }
    public void cancelActive() { core.cancelActive(); }
    public boolean isActive() { return core.isActive(); }
    @Override public void close() { core.close(); }

    private void requireIdle() { if (core.isActive()) throw new IllegalStateException("Agent 正在执行，暂不能切换会话"); }
    private void requireSessions() { if (!sessionsConfig.enabled()) throw new IllegalStateException("会话持久化功能已关闭"); }

    private SessionSnapshot ephemeral(List<ChatMessage> history) {
        var id = current == null ? SessionId.generate() : current.metadata().id();
        var created = current == null ? clock.instant() : current.metadata().createdAt();
        return new SessionSnapshot(new SessionMetadata(id, created, clock.instant(), "ephemeral", 0, history.size()), history);
    }
    private SessionSnapshot newEphemeral() {
        var id = SessionId.generate();
        return new SessionSnapshot(new SessionMetadata(id, clock.instant(), clock.instant(),
                "ephemeral", 0, 0), List.of());
    }
    private static SessionSummary summary(SessionSnapshot snapshot) {
        var meta = snapshot.metadata(); return new SessionSummary(meta.id(), meta.createdAt(), meta.updatedAt(), meta.messageCount());
    }
    private static ConversationListener noopConversationListener() {
        return new ConversationListener() {
            @Override public void onTextDelta(String text) { }
            @Override public void onAgentEvent(AgentEvent event) { }
        };
    }
}
