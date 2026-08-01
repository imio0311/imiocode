package io.imiocode.agent;

import io.imiocode.config.AgentConfig;
import io.imiocode.config.ContextConfig;
import io.imiocode.context.ApproximateTokenEstimator;
import io.imiocode.context.ContextManager;
import io.imiocode.context.ConversationSerializer;
import io.imiocode.context.ConversationSummarizer;
import io.imiocode.context.SummaryParser;
import io.imiocode.context.ToolResultOffloader;
import io.imiocode.context.ToolResultSpillStore;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.TokenUsage;
import io.imiocode.llm.LlmException;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentContextManagementTest {
    @TempDir Path workspace;

    @Test
    void autoCompactsBeforeModelRequestAndReturnsManagedHistory() {
        SequencedClient client = new SequencedClient(false);
        Agent agent = agent(client, new ContextConfig(2_000, 0.50));
        List<AgentEvent> events = new ArrayList<>();

        AgentResult result = agent.run(new AgentRequest(List.of(),
                new ChatMessage(MessageRole.USER, "x".repeat(8_000)), List.of()), events::add);

        assertTrue(result.completed(), result.toString());
        assertEquals(1, client.summaryCalls);
        assertEquals(1, client.normalCalls);
        assertTrue(client.lastNormalRequest.messages().getFirst().content().contains("Compacted conversation summary"));
        assertTrue(events.stream().anyMatch(AgentEvent.ContextChanged.class::isInstance));
        agent.close();
    }

    @Test
    void contextLimitTriggersExactlyOneRecoveryAndRetry() {
        SequencedClient client = new SequencedClient(true);
        Agent agent = agent(client, new ContextConfig(64_000, 0.80));

        AgentResult result = agent.run(new AgentRequest(List.of(),
                new ChatMessage(MessageRole.USER, "x".repeat(8_000)), List.of()), AgentEventListener.NOOP);

        assertTrue(result.completed(), result.toString());
        assertEquals(1, client.summaryCalls);
        assertEquals(2, client.normalCalls);
        agent.close();
    }

    private Agent agent(SequencedClient client, ContextConfig contextConfig) {
        ToolRegistry registry = new ToolRegistry();
        PromptAssembler assembler = new PromptAssembler(SystemPromptBuilder.defaults(), registry);
        SecretRedactor redactor = new SecretRedactor("");
        ContextManager manager = new ContextManager(contextConfig, 100, assembler,
                new ApproximateTokenEstimator(),
                new ToolResultOffloader(new ToolResultSpillStore(workspace, redactor), redactor),
                new ConversationSummarizer(client, new ConversationSerializer(), new SummaryParser()));
        return new Agent(client, registry,
                new AgentConfig(5, Duration.ofSeconds(5), 2), 100,
                () -> new EnvironmentContext(workspace, "Windows", "x64", "powershell",
                        ZonedDateTime.now(), GitContext.unavailable(), "test"),
                new EnvironmentReminderFormatter(), null, manager);
    }

    private static final class SequencedClient implements LlmClient {
        private final boolean failFirstNormal;
        private int normalCalls;
        private int summaryCalls;
        private ChatRequest lastNormalRequest;
        private SequencedClient(boolean failFirstNormal) { this.failFirstNormal = failFirstNormal; }

        @Override
        public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) throws LlmException {
            if (request.systemPromptOverride().isPresent()) {
                summaryCalls++;
                return new ChatResponse("<summary><prior_history>既有目标</prior_history><active_task>继续完成长任务</active_task></summary>");
            }
            normalCalls++;
            lastNormalRequest = request;
            if (failFirstNormal && normalCalls == 1) {
                throw new LlmException(LlmErrorType.CONTEXT_LIMIT, true, 400, "输入上下文超过模型窗口限制");
            }
            listener.onEvent(new LlmEvent.TextDelta("任务完成"));
            listener.onEvent(new LlmEvent.StreamCompleted(TokenUsage.unknown()));
            return new ChatResponse("任务完成");
        }

        @Override public void close() { }
    }
}
