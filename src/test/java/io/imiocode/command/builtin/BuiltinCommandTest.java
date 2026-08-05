package io.imiocode.command.builtin;

import io.imiocode.agent.AgentMode;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandOutcome;
import io.imiocode.command.CommandRegistry;
import io.imiocode.command.CommandServices;
import io.imiocode.command.CommandStatus;
import io.imiocode.command.ConfirmationPrompt;
import io.imiocode.command.UIController;
import io.imiocode.config.UiVerbosity;
import io.imiocode.context.CompactReport;
import io.imiocode.context.ContextOutcome;
import io.imiocode.memory.MemoryDocument;
import io.imiocode.memory.MemoryCategory;
import io.imiocode.memory.MemoryEntry;
import io.imiocode.memory.MemoryScope;
import io.imiocode.permission.PermissionMode;
import io.imiocode.session.SessionId;
import io.imiocode.session.SessionLoadResult;
import io.imiocode.session.SessionMetadata;
import io.imiocode.session.SessionSnapshot;
import io.imiocode.session.SessionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltinCommandTest {
    private CommandRegistry registry;
    private FakeServices services;
    private FakeUi ui;
    private CommandContext context;

    @BeforeEach
    void setUp() {
        registry = new CommandRegistry();
        registry.register(new HelpCommand());
        registry.register(new CompactCommand());
        registry.register(new ClearCommand());
        registry.register(new PlanCommand());
        registry.register(new DoCommand());
        registry.register(new SessionCommand());
        registry.register(new MemoryCommand());
        registry.register(new PermissionCommand());
        registry.register(new StatusCommand());
        registry.register(new ReviewCommand());
        registry.register(new ExitCommand());
        registry.register(new VerbosityCommand("verbose", UiVerbosity.VERBOSE));
        registry.register(new VerbosityCommand("compact-ui", UiVerbosity.COMPACT));
        services = new FakeServices();
        ui = new FakeUi();
        context = new CommandContext(services, ui, registry);
    }

    @Test
    void registryContainsTenCoreCommandsAndCompatibilityAliases() {
        assertEquals(10, registry.listCommands().stream().filter(item -> !item.compatibility()).count());
        assertEquals(3, registry.listCommands().stream().filter(item -> item.compatibility()).count());
        for (String value : List.of("h", "?", "cls", "sessions", "mem", "perm", "st", "rv", "quit")) {
            assertTrue(registry.find(value).isPresent(), value);
        }
        assertEquals(registry.find("exit").orElseThrow(), registry.find("quit").orElseThrow());
    }

    @Test
    void helpBuildsItsCatalogFromRegistry() {
        String text = dispatch("/help").messages().getFirst().text();
        assertTrue(text.contains("核心命令："));
        assertTrue(text.contains("/permission"));
        assertTrue(text.contains("/review"));
        assertTrue(text.contains("兼容命令："));
        assertTrue(text.contains("/exit 或 /quit"));
    }

    @Test
    void clearOnlyTouchesUi() {
        var result = dispatch("/clear");
        assertEquals(CommandOutcome.HANDLED, result.outcome());
        assertEquals(1, ui.clearCalls);
        assertEquals(0, services.calls);
        assertTrue(result.prompt().isEmpty());
    }

    @Test
    void permissionQueriesAndSwitchesOnlyRuntimeMode() {
        assertTrue(dispatch("/permission").messages().getFirst().text().contains("ask"));
        assertEquals(PermissionMode.ASK, services.permission);

        dispatch("/perm full-access");
        assertEquals(PermissionMode.FULL_ACCESS, services.permission);
        assertTrue(dispatch("/permission bad").messages().getFirst().error());
        assertTrue(dispatch("/permission ask extra").messages().getFirst().text().contains("用法："));

        for (String value : List.of("ask", "auto-edit", "read-only", "full-access", "lockdown")) {
            assertFalse(dispatch("/permission " + value).messages().getFirst().error());
        }
    }

    @Test
    void compactUsesDedicatedServiceAndReviewOnlyReturnsPrompt() {
        var compact = dispatch("/compact");
        assertEquals(1, services.compactCalls);
        assertTrue(compact.messages().getFirst().text().contains("100 → 50"));

        int callsBefore = services.calls;
        var review = dispatch("/review 关注 并发安全");
        assertEquals(CommandOutcome.FORWARD_TO_AGENT, review.outcome());
        assertTrue(review.prompt().orElseThrow().contains("Additional focus:\n关注 并发安全"));
        assertFalse(review.prompt().orElseThrow().contains("/review"));
        assertEquals(callsBefore, services.calls);
        assertEquals(AgentMode.DO, services.mode);

        services.mode = AgentMode.PLAN;
        dispatch("/review");
        assertEquals(AgentMode.PLAN, services.mode);
    }

    @Test
    void statusContainsSafeRuntimeFields() {
        String text = dispatch("/status").messages().getFirst().text();
        assertTrue(text.contains("Provider: deepseek"));
        assertTrue(text.contains("Context: 8.2k/64k tokens"));
        assertTrue(text.contains("MCP: 1 servers · 2 tools"));
        assertFalse(text.toLowerCase().contains("api-key"));
        assertFalse(text.contains("secret-value"));
    }

    @Test
    void sessionAndMemorySubcommandsKeepTheirLocalBehavior() {
        assertTrue(dispatch("/session current").messages().getFirst().text().contains(FakeServices.SESSION.value()));
        assertTrue(dispatch("/session list").messages().getFirst().text().contains("项目会话"));
        assertTrue(dispatch("/session new").messages().getFirst().text().contains("已新建"));
        assertTrue(dispatch("/session resume 111111111111111111111111").messages().getFirst().text().contains("已恢复"));

        ui.confirmation = false;
        assertTrue(dispatch("/session delete 222222222222222222222222").messages().getFirst().text().contains("取消"));
        assertEquals(0, services.deleteCalls);
        ui.confirmation = true;
        dispatch("/session delete 222222222222222222222222");
        assertEquals(1, services.deleteCalls);

        assertTrue(dispatch("/memory list").messages().getFirst().text().contains("project"));
        assertTrue(dispatch("/memory add project 长期事实").messages().getFirst().text().contains("已添加"));
        assertTrue(dispatch("/memory edit project m_111111111111 新事实").messages().getFirst().text().contains("已更新"));
        assertTrue(dispatch("/memory forget project m_111111111111").messages().getFirst().text().contains("已删除"));
        assertTrue(dispatch("/session").messages().getFirst().text().contains("用法："));
        assertTrue(dispatch("/memory").messages().getFirst().text().contains("用法："));
    }

    @Test
    void compatibilityCommandsWorkWithOnlyAbstractUi() {
        assertEquals(CommandOutcome.EXIT_REQUESTED, dispatch("/quit").outcome());
        assertEquals(CommandOutcome.HANDLED, dispatch("/verbose").outcome());
        assertEquals(UiVerbosity.VERBOSE, ui.verbosity());
        assertEquals(CommandOutcome.HANDLED, dispatch("/compact-ui").outcome());
        assertEquals(UiVerbosity.COMPACT, ui.verbosity());
        assertEquals(AgentMode.DO, services.mode);
    }

    private io.imiocode.command.CommandResult dispatch(String input) {
        return registry.dispatch(input, context).orElseThrow();
    }

    private static final class FakeUi implements UIController {
        private int clearCalls;
        private boolean confirmation;
        private UiVerbosity verbosity = UiVerbosity.COMPACT;
        @Override public void clearScreen() { clearCalls++; }
        @Override public boolean confirm(ConfirmationPrompt prompt) { return confirmation; }
        @Override public UiVerbosity verbosity() { return verbosity; }
        @Override public void setVerbosity(UiVerbosity verbosity) { this.verbosity = verbosity; }
        @Override public void refreshStatus(CommandStatus status) { }
    }

    private static final class FakeServices implements CommandServices {
        private static final SessionId SESSION = new SessionId("0123456789abcdef01234567");
        private AgentMode mode = AgentMode.DO;
        private PermissionMode permission = PermissionMode.ASK;
        private int compactCalls;
        private int calls;
        private int deleteCalls;

        @Override public AgentMode mode() { calls++; return mode; }
        @Override public void switchMode(AgentMode mode) { calls++; this.mode = mode; }
        @Override public CompactReport compact() {
            calls++; compactCalls++;
            return new CompactReport(100, 50, 0, true, ContextOutcome.COMPACTED, "完成");
        }
        @Override public SessionSummary currentSession() { calls++; return summary(); }
        @Override public List<SessionSummary> listSessions() { calls++; return List.of(summary()); }
        @Override public SessionSummary newSession() { calls++; return summary(); }
        @Override public SessionLoadResult resumeSession(SessionId id) {
            calls++;
            return SessionLoadResult.clean(new SessionSnapshot(
                    new SessionMetadata(id, Instant.EPOCH, Instant.EPOCH, "test", 0, 0), List.of()));
        }
        @Override public void deleteSession(SessionId id) { calls++; deleteCalls++; }
        @Override public boolean sessionsEnabled() { calls++; return true; }
        @Override public List<MemoryDocument> listMemories(Optional<MemoryScope> scope) {
            calls++;
            return List.of(new MemoryDocument(MemoryScope.PROJECT,
                    List.of(new MemoryEntry("m_111111111111", MemoryCategory.PROJECT_FACT, "事实"))));
        }
        @Override public MemoryEntry addMemory(MemoryScope scope, String content) {
            calls++; return new MemoryEntry("m_222222222222", MemoryCategory.PROJECT_FACT, content);
        }
        @Override public MemoryEntry editMemory(MemoryScope scope, String id, String content) {
            calls++; return new MemoryEntry(id, MemoryCategory.PROJECT_FACT, content);
        }
        @Override public void forgetMemory(MemoryScope scope, String id) { calls++; }
        @Override public boolean memoryEnabled() { calls++; return true; }
        @Override public PermissionMode permissionMode() { calls++; return permission; }
        @Override public void switchPermissionMode(PermissionMode mode) { calls++; permission = mode; }
        @Override public CommandStatus status() {
            calls++;
            return new CommandStatus("deepseek", "deepseek-chat", Path.of("."), mode, permission,
                    SESSION, 8_200, 64_000, 1, 2);
        }
        private static SessionSummary summary() {
            return new SessionSummary(SESSION, Instant.EPOCH, Instant.EPOCH, 0);
        }
    }
}
