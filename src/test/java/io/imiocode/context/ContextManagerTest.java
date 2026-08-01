package io.imiocode.context;

import io.imiocode.config.ContextConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.prompt.PromptAssembler;
import io.imiocode.prompt.SystemPromptBuilder;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextManagerTest {
    @TempDir Path workspace;

    @Test
    void doesNotSummarizeBelowThreshold() {
        StubClient client = new StubClient(validSummary());
        ContextManager manager = manager(new ContextConfig(64_000, 0.80), client);
        ContextRequest request = request(List.of(new ChatMessage(MessageRole.USER, "短消息")), ContextManageMode.AUTO,
                new AutoCompactTrackingState());

        ContextResult result = manager.manage(request, ContextEventListener.NOOP);

        assertEquals(ContextOutcome.UNCHANGED, result.outcome());
        assertEquals(0, client.calls);
        assertFalse(result.compacted());
    }

    @Test
    void autoCompactsAboveThresholdAndEmitsEvents() {
        StubClient client = new StubClient(validSummary());
        ContextManager manager = manager(new ContextConfig(2_000, 0.50), client);
        List<ContextEvent> events = new ArrayList<>();
        ContextRequest request = request(List.of(new ChatMessage(MessageRole.USER, "x".repeat(5_000))),
                ContextManageMode.AUTO, new AutoCompactTrackingState());

        ContextResult result = manager.manage(request, events::add);

        assertEquals(ContextOutcome.COMPACTED, result.outcome());
        assertTrue(result.afterTokens() < result.beforeTokens());
        assertEquals(1, client.calls);
        assertTrue(events.stream().anyMatch(ContextEvent.Started.class::isInstance));
        assertTrue(events.stream().anyMatch(ContextEvent.Completed.class::isInstance));
        assertEquals(2, result.workingMessages().size());
    }

    @Test
    void forceCompactsBelowThresholdAndFailureCircuitIsBounded() {
        StubClient forceClient = new StubClient(validSummary());
        ContextManager forceManager = manager(new ContextConfig(64_000, 0.80), forceClient);
        ContextResult forced = forceManager.manage(request(
                List.of(new ChatMessage(MessageRole.USER, "x".repeat(10_000))), ContextManageMode.FORCE,
                new AutoCompactTrackingState()), ContextEventListener.NOOP);
        assertEquals(ContextOutcome.COMPACTED, forced.outcome());

        StubClient invalid = new StubClient("invalid");
        ContextManager autoManager = manager(new ContextConfig(1_000, 0.50), invalid);
        AutoCompactTrackingState tracking = new AutoCompactTrackingState();
        ContextRequest auto = request(List.of(new ChatMessage(MessageRole.USER, "x".repeat(5_000))),
                ContextManageMode.AUTO, tracking);
        assertEquals(ContextOutcome.FAILED, autoManager.manage(auto, ContextEventListener.NOOP).outcome());
        assertEquals(ContextOutcome.FAILED, autoManager.manage(auto, ContextEventListener.NOOP).outcome());
        assertEquals(ContextOutcome.CIRCUIT_OPEN, autoManager.manage(auto, ContextEventListener.NOOP).outcome());
        assertEquals(ContextOutcome.CIRCUIT_OPEN, autoManager.manage(auto, ContextEventListener.NOOP).outcome());
        assertEquals(3, invalid.calls);
    }

    private ContextManager manager(ContextConfig config, StubClient client) {
        PromptAssembler assembler = new PromptAssembler(SystemPromptBuilder.defaults(), new ToolRegistry());
        SecretRedactor redactor = new SecretRedactor("");
        ToolResultSpillStore store = new ToolResultSpillStore(workspace, redactor);
        return new ContextManager(config, 100, assembler, new ApproximateTokenEstimator(),
                new ToolResultOffloader(store, redactor),
                new ConversationSummarizer(client, new ConversationSerializer(), new SummaryParser()));
    }

    private static ContextRequest request(List<ChatMessage> messages, ContextManageMode mode,
                                          AutoCompactTrackingState tracking) {
        return new ContextRequest(messages, List.of(), List.of(), ToolSelection.only(java.util.Set.of()),
                OptionalInt.of(100), mode, tracking);
    }

    private static String validSummary() {
        return "<summary><prior_history>用户目标与约束</prior_history><active_task>继续当前任务</active_task></summary>";
    }

    private static final class StubClient implements LlmClient {
        private final String response;
        private int calls;
        private StubClient(String response) { this.response = response; }
        @Override public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) {
            calls++;
            return new ChatResponse(response);
        }
        @Override public void close() { }
    }
}
