package io.imiocode.runtime;

import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentEvent;
import io.imiocode.command.CommandServices;
import io.imiocode.command.CommandStatus;
import io.imiocode.config.MemoryConfig;
import io.imiocode.config.SessionsConfig;
import io.imiocode.context.CompactReport;
import io.imiocode.context.ApproximateTokenEstimator;
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
import io.imiocode.hook.HookEvent;
import io.imiocode.hook.HookNotification;
import io.imiocode.hook.HookRuntime;
import io.imiocode.hook.integration.HookContextFactory;
import io.imiocode.persistence.PersistenceEvent;
import io.imiocode.persistence.PersistenceEventListener;
import io.imiocode.persistence.PersistentContextProvider;
import io.imiocode.permission.PermissionReply;
import io.imiocode.permission.PermissionMode;
import io.imiocode.permission.RuntimePermissionSettings;
import io.imiocode.session.SessionId;
import io.imiocode.session.SessionLoadResult;
import io.imiocode.session.SessionManager;
import io.imiocode.session.SessionMetadata;
import io.imiocode.session.SessionSnapshot;
import io.imiocode.session.SessionSummary;
import io.imiocode.skill.SkillCatalogSnapshot;
import io.imiocode.skill.SkillCommandRegistrar;
import io.imiocode.skill.SkillExecutor;
import io.imiocode.skill.SkillInvocation;
import io.imiocode.skill.SkillMode;
import io.imiocode.skill.SkillSummaryFormatter;
import io.imiocode.skill.install.SkillInstallListener;
import io.imiocode.skill.install.SkillInstallRequest;
import io.imiocode.skill.install.SkillInstallResult;
import io.imiocode.skill.install.SkillInstaller;

import java.time.Clock;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.URI;

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
    private final RuntimePermissionSettings permissionSettings;
    private final ApproximateTokenEstimator tokenEstimator;
    private final String provider;
    private final String model;
    private final Path workspace;
    private final long contextWindowTokens;
    private final int connectedMcpServers;
    private final int registeredMcpTools;
    private final SkillExecutor skillExecutor;
    private final SkillSummaryFormatter skillSummaryFormatter;
    private final SkillCommandRegistrar skillCommandRegistrar;
    private final SkillInstaller skillInstaller;
    private final HookRuntime hooks;
    private final HookContextFactory hookContexts;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<SkillInvocation> pendingSkill = new AtomicReference<>();
    private volatile long synchronizedSkillGeneration = -1;
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
            Clock clock,
            RuntimePermissionSettings permissionSettings,
            ApproximateTokenEstimator tokenEstimator,
            String provider,
            String model,
            Path workspace,
            long contextWindowTokens,
            int connectedMcpServers,
            int registeredMcpTools) {
        this(core, sessions, sessionsConfig, memories, memoryConfig, extractor, context, events,
                clock, permissionSettings, tokenEstimator, provider, model, workspace,
                contextWindowTokens, connectedMcpServers, registeredMcpTools, null, null, null, null,
                HookRuntime.NOOP, new HookContextFactory(workspace));
    }

    public ConversationCoordinator(
            ConversationSession core,
            SessionManager sessions,
            SessionsConfig sessionsConfig,
            MemoryManager memories,
            MemoryConfig memoryConfig,
            MemoryExtractor extractor,
            PersistentContextProvider context,
            PersistenceEventListener events,
            Clock clock,
            RuntimePermissionSettings permissionSettings,
            ApproximateTokenEstimator tokenEstimator,
            String provider,
            String model,
            Path workspace,
            long contextWindowTokens,
            int connectedMcpServers,
            int registeredMcpTools,
            SkillExecutor skillExecutor,
            SkillSummaryFormatter skillSummaryFormatter,
            SkillCommandRegistrar skillCommandRegistrar) {
        this(core, sessions, sessionsConfig, memories, memoryConfig, extractor, context, events,
                clock, permissionSettings, tokenEstimator, provider, model, workspace,
                contextWindowTokens, connectedMcpServers, registeredMcpTools,
                skillExecutor, skillSummaryFormatter, skillCommandRegistrar, null,
                HookRuntime.NOOP, new HookContextFactory(workspace));
    }

    public ConversationCoordinator(
            ConversationSession core,
            SessionManager sessions,
            SessionsConfig sessionsConfig,
            MemoryManager memories,
            MemoryConfig memoryConfig,
            MemoryExtractor extractor,
            PersistentContextProvider context,
            PersistenceEventListener events,
            Clock clock,
            RuntimePermissionSettings permissionSettings,
            ApproximateTokenEstimator tokenEstimator,
            String provider,
            String model,
            Path workspace,
            long contextWindowTokens,
            int connectedMcpServers,
            int registeredMcpTools,
            SkillExecutor skillExecutor,
            SkillSummaryFormatter skillSummaryFormatter,
            SkillCommandRegistrar skillCommandRegistrar,
            SkillInstaller skillInstaller) {
        this(core, sessions, sessionsConfig, memories, memoryConfig, extractor, context, events,
                clock, permissionSettings, tokenEstimator, provider, model, workspace,
                contextWindowTokens, connectedMcpServers, registeredMcpTools,
                skillExecutor, skillSummaryFormatter, skillCommandRegistrar, skillInstaller,
                HookRuntime.NOOP, new HookContextFactory(workspace));
    }

    public ConversationCoordinator(
            ConversationSession core,
            SessionManager sessions,
            SessionsConfig sessionsConfig,
            MemoryManager memories,
            MemoryConfig memoryConfig,
            MemoryExtractor extractor,
            PersistentContextProvider context,
            PersistenceEventListener events,
            Clock clock,
            RuntimePermissionSettings permissionSettings,
            ApproximateTokenEstimator tokenEstimator,
            String provider,
            String model,
            Path workspace,
            long contextWindowTokens,
            int connectedMcpServers,
            int registeredMcpTools,
            SkillExecutor skillExecutor,
            SkillSummaryFormatter skillSummaryFormatter,
            SkillCommandRegistrar skillCommandRegistrar,
            SkillInstaller skillInstaller,
            HookRuntime hooks,
            HookContextFactory hookContexts) {
        this.core = core; this.sessions = sessions; this.sessionsConfig = sessionsConfig;
        this.memories = memories; this.memoryConfig = memoryConfig; this.extractor = extractor;
        this.context = context; this.events = events == null ? PersistenceEventListener.noop() : events;
        this.clock = clock;
        this.permissionSettings = java.util.Objects.requireNonNull(permissionSettings, "permissionSettings");
        this.tokenEstimator = java.util.Objects.requireNonNull(tokenEstimator, "tokenEstimator");
        this.provider = requireText(provider, "provider");
        this.model = requireText(model, "model");
        this.workspace = java.util.Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize();
        if (contextWindowTokens <= 0) throw new IllegalArgumentException("contextWindowTokens 必须为正数");
        if (connectedMcpServers < 0 || registeredMcpTools < 0) throw new IllegalArgumentException("MCP 计数不能为负数");
        this.contextWindowTokens = contextWindowTokens;
        this.connectedMcpServers = connectedMcpServers;
        this.registeredMcpTools = registeredMcpTools;
        this.skillExecutor = skillExecutor;
        this.skillSummaryFormatter = skillSummaryFormatter;
        this.skillCommandRegistrar = skillCommandRegistrar;
        this.skillInstaller = skillInstaller;
        this.hooks = java.util.Objects.requireNonNullElse(hooks, HookRuntime.NOOP);
        this.hookContexts = java.util.Objects.requireNonNull(hookContexts, "hookContexts");
        this.current = sessionsConfig.enabled() ? sessions.createNew() : ephemeral(List.of());
        if (sessionsConfig.enabled()) sessions.applyRetention(current.metadata().id());
        startSession(current);
    }

    public ChatResponse sendWithEvents(String userInput, ConversationListener listener) throws ConversationException {
        ChatMessage userMessage = new ChatMessage(io.imiocode.conversation.MessageRole.USER, userInput);
        List<io.imiocode.conversation.SystemReminder> reminders = new java.util.ArrayList<>(context.currentReminders());
        if (skillExecutor != null && skillSummaryFormatter != null) {
            reminders.add(skillSummaryFormatter.format(skillCatalog()));
        }
        SkillInvocation invocation = pendingSkill.getAndSet(null);
        ChatResponse response;
        if (invocation != null && invocation.skill().metadata().mode() == SkillMode.FORK) {
            List<ChatMessage> forkHistory = new java.util.ArrayList<>();
            forkHistory.addAll(core.historySnapshot());
            reminders.forEach(reminder -> forkHistory.add(new ChatMessage(
                    io.imiocode.conversation.MessageRole.USER, reminder.wrappedContent())));
            String result = skillExecutor.executeFork(
                    invocation, forkHistory, event -> listener.onAgentEvent(event));
            response = core.appendForkResult(userInput, result);
        } else {
            reminders.forEach(reminder -> core.addSystemReminder(reminder.content()));
            response = invocation == null
                    ? core.sendWithEvents(userInput, listener)
                    : core.sendSkillWithEvents(userInput, invocation, listener);
        }
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
        requireIdle();
        core.switchMode(mode, noopConversationListener());
    }

    @Override
    public CompactReport compact() {
        requireIdle();
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
        endCurrentSession();
        core.replaceHistory(List.of()); current = next;
        context.reloadInstructions(); context.reloadMemories();
        startSession(next);
        return summary(next);
    }

    @Override
    public SessionLoadResult resumeSession(SessionId id) {
        requireSessions(); requireIdle();
        SessionLoadResult loaded = sessions.load(id);
        endCurrentSession();
        core.replaceHistory(loaded.snapshot().history()); current = loaded.snapshot();
        context.reloadInstructions(); context.reloadMemories();
        startSession(current);
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

    @Override public PermissionMode permissionMode() { return permissionSettings.mode(); }

    @Override
    public String prepareSkillInvocation(String name, String arguments) {
        requireIdle();
        if (skillExecutor == null) throw new IllegalStateException("Skill 功能尚未初始化");
        SkillInvocation invocation = skillExecutor.prepare(name, arguments);
        if (!pendingSkill.compareAndSet(null, invocation)) {
            throw new IllegalStateException("已有待执行 Skill");
        }
        return "/" + invocation.skill().metadata().name()
                + (arguments == null || arguments.isBlank() ? "" : " " + arguments.trim());
    }

    @Override
    public SkillCatalogSnapshot skillCatalog() {
        if (skillExecutor == null) return SkillCatalogSnapshot.empty();
        SkillCatalogSnapshot snapshot = skillExecutor.loader().snapshot();
        syncSkillCommands(snapshot);
        return snapshot;
    }

    @Override
    public SkillCatalogSnapshot reloadSkills() {
        requireIdle();
        if (skillExecutor == null) throw new IllegalStateException("Skill 功能尚未初始化");
        SkillCatalogSnapshot snapshot = skillExecutor.loader().reload();
        syncSkillCommands(snapshot);
        return snapshot;
    }

    @Override
    public SkillInstallResult installSkill(String url, boolean force, SkillInstallListener listener) {
        requireIdle();
        if (skillInstaller == null) throw new IllegalStateException("Skill 远程安装功能尚未初始化");
        PermissionMode mode = permissionSettings.mode();
        if (mode == PermissionMode.LOCKDOWN || mode == PermissionMode.READ_ONLY) {
            throw new IllegalStateException("当前权限模式禁止安装 Skill");
        }
        SkillInstallResult result = skillInstaller.install(
                new SkillInstallRequest(URI.create(url), force), listener);
        syncSkillCommands(skillExecutor.loader().snapshot());
        return result;
    }

    @Override
    public void switchPermissionMode(PermissionMode mode) {
        requireIdle();
        permissionSettings.switchMode(mode);
    }

    @Override
    public CommandStatus status() {
        List<ChatMessage> history = core.historySnapshot();
        return new CommandStatus(
                provider,
                model,
                workspace,
                core.mode(),
                permissionSettings.mode(),
                current.metadata().id(),
                tokenEstimator.estimateMessages(history),
                contextWindowTokens,
                connectedMcpServers,
                registeredMcpTools);
    }

    public boolean respondPermission(String requestId, PermissionReply reply) { return core.respondPermission(requestId, reply); }
    public List<HookNotification> drainHookNotifications() { return hooks.drainNotifications(); }
    public void cancelActive() {
        core.cancelActive();
        if (skillExecutor != null) skillExecutor.cancelFork();
        if (skillInstaller != null) skillInstaller.cancel();
    }
    @Override public void cancelActiveWork() { cancelActive(); }
    public boolean isActive() {
        return core.isActive() || (skillExecutor != null && skillExecutor.isForkActive());
    }
    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        cancelActive();
        try { endCurrentSession(); }
        catch (RuntimeException ignored) {
            // 关闭不能被 session_end Hook 失败阻塞。
            hooks.clearPrompts();
            hookContexts.clearSessionId();
        }
        if (skillInstaller != null) skillInstaller.close();
        core.close();
    }

    private void startSession(SessionSnapshot snapshot) {
        hookContexts.setSessionId(snapshot.metadata().id().value());
        hooks.runHooks(hookContexts.builder(HookEvent.SESSION_START).build());
    }

    private void endCurrentSession() {
        if (current == null) return;
        hooks.runHooks(hookContexts.builder(HookEvent.SESSION_END).build());
        hooks.clearPrompts();
        hookContexts.clearSessionId();
    }

    private void requireIdle() { if (core.isActive()) throw new IllegalStateException("Agent 正在执行，暂不能切换会话"); }
    private void requireSessions() { if (!sessionsConfig.enabled()) throw new IllegalStateException("会话持久化功能已关闭"); }

    private void syncSkillCommands(SkillCatalogSnapshot snapshot) {
        if (skillCommandRegistrar != null && synchronizedSkillGeneration != snapshot.generation()) {
            skillCommandRegistrar.sync(snapshot);
            synchronizedSkillGeneration = snapshot.generation();
        }
    }

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

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " 不能为空");
        return value.trim();
    }
}
