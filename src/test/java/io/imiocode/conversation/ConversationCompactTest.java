package io.imiocode.conversation;

import io.imiocode.agent.Agent;
import io.imiocode.config.AgentConfig;
import io.imiocode.config.ContextConfig;
import io.imiocode.context.ApproximateTokenEstimator;
import io.imiocode.context.CompactReport;
import io.imiocode.context.ContextManager;
import io.imiocode.context.ContextOutcome;
import io.imiocode.context.ConversationSerializer;
import io.imiocode.context.ConversationSummarizer;
import io.imiocode.context.SummaryParser;
import io.imiocode.context.ToolResultOffloader;
import io.imiocode.context.ToolResultSpillStore;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.TokenUsage;
import io.imiocode.prompt.EnvironmentContext;
import io.imiocode.prompt.EnvironmentReminderFormatter;
import io.imiocode.prompt.GitContext;
import io.imiocode.prompt.PromptAssembler;
import io.imiocode.prompt.SystemPromptBuilder;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationCompactTest {
    @TempDir Path workspace;

    @Test
    void forceCompactReplacesHistoryAndReportsTokens() throws Exception {
        CompactClient client = new CompactClient(false);
        ConversationSession session = session(client);
        session.sendWithEvents("x".repeat(8_000), text -> { });
        int beforeMessages = session.historySnapshot().size();

        CompactReport report = session.forceCompact(text -> { });

        assertTrue(report.compacted());
        assertTrue(report.afterTokens() < report.beforeTokens());
        assertEquals(2, session.historySnapshot().size());
        assertTrue(beforeMessages >= 2);
        assertEquals(1, client.summaryCalls);
        session.close();
    }

    @Test
    void failedCompactKeepsHistoryAndEmptyHistoryDoesNotCallLlm() throws Exception {
        CompactClient client = new CompactClient(true);
        ConversationSession empty = session(client);
        CompactReport emptyReport = empty.forceCompact(text -> { });
        assertEquals(ContextOutcome.UNCHANGED, emptyReport.outcome());
        assertEquals(0, client.summaryCalls);

        empty.sendWithEvents("x".repeat(8_000), text -> { });
        List<ChatMessage> original = empty.historySnapshot();
        CompactReport failed = empty.forceCompact(text -> { });
        assertFalse(failed.compacted());
        assertEquals(original, empty.historySnapshot());
        assertEquals(1, client.summaryCalls);
        empty.close();
    }

    private ConversationSession session(CompactClient client) {
        ToolRegistry registry = new ToolRegistry();
        PromptAssembler assembler = new PromptAssembler(SystemPromptBuilder.defaults(), registry);
        SecretRedactor redactor = new SecretRedactor("");
        ContextManager manager = new ContextManager(new ContextConfig(64_000, 0.80), 100,
                assembler, new ApproximateTokenEstimator(),
                new ToolResultOffloader(new ToolResultSpillStore(workspace, redactor), redactor),
                new ConversationSummarizer(client, new ConversationSerializer(), new SummaryParser()));
        Agent agent = new Agent(client, registry, new AgentConfig(5, Duration.ofSeconds(5), 2), 100,
                () -> new EnvironmentContext(workspace, "Windows", "x64", "powershell",
                        ZonedDateTime.now(), GitContext.unavailable(), "test"),
                new EnvironmentReminderFormatter(), null, manager);
        return new ConversationSession(agent);
    }

    private static final class CompactClient implements LlmClient {
        private final boolean invalidSummary;
        private int summaryCalls;
        private CompactClient(boolean invalidSummary) { this.invalidSummary = invalidSummary; }

        @Override
        public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) {
            if (request.systemPromptOverride().isPresent()) {
                summaryCalls++;
                return new ChatResponse(invalidSummary ? "invalid"
                        : "<summary><prior_history>保留用户目标</prior_history><active_task>当前任务已完成</active_task></summary>");
            }
            listener.onEvent(new LlmEvent.TextDelta("完成"));
            listener.onEvent(new LlmEvent.StreamCompleted(TokenUsage.unknown()));
            return new ChatResponse("完成");
        }

        @Override public void close() { }
    }
}
