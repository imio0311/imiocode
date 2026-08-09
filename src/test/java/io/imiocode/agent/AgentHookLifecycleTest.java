package io.imiocode.agent;

import io.imiocode.config.AgentConfig;
import io.imiocode.conversation.*;
import io.imiocode.hook.*;
import io.imiocode.hook.integration.HookContextFactory;
import io.imiocode.llm.*;
import io.imiocode.prompt.*;
import io.imiocode.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentHookLifecycleTest {
    @Test void normalTaskEmitsLifecycleAndTurnPromptReachesCurrentProviderRequest() {
        CapturingClient client = new CapturingClient(false);
        RecordingHooks hooks = new RecordingHooks();
        try (Agent agent = agent(client, hooks)) {
            AgentResult result = agent.run(new AgentRequest(
                    new ChatMessage(MessageRole.USER, "hello")), AgentEventListener.NOOP);
            assertTrue(result.completed());
        }
        assertEquals(List.of(HookEvent.TURN_START, HookEvent.PRE_SEND,
                HookEvent.POST_RECEIVE, HookEvent.TURN_END), hooks.events);
        assertTrue(client.request.reminders().stream()
                .anyMatch(reminder -> reminder.content().equals("HOOK_TURN_PROMPT")));
    }

    @Test void finalModelFailureEmitsErrorThenTurnEndWithoutRecursion() {
        CapturingClient client = new CapturingClient(true);
        RecordingHooks hooks = new RecordingHooks();
        try (Agent agent = agent(client, hooks)) {
            AgentResult result = agent.run(new AgentRequest(
                    new ChatMessage(MessageRole.USER, "fail")), AgentEventListener.NOOP);
            assertFalse(result.completed());
        }
        assertEquals(HookEvent.TURN_START, hooks.events.getFirst());
        assertEquals(4, hooks.events.stream().filter(event -> event == HookEvent.PRE_SEND).count());
        assertEquals(List.of(HookEvent.ERROR, HookEvent.TURN_END),
                hooks.events.subList(hooks.events.size() - 2, hooks.events.size()));
    }

    private static Agent agent(LlmClient client, HookRuntime hooks) {
        Path workspace = Path.of(".").toAbsolutePath();
        EnvironmentContextProvider environment = () -> new EnvironmentContext(
                workspace, "test-os", "test-arch", "test-shell", ZonedDateTime.now(),
                GitContext.unavailable(), "test-model");
        return new Agent(client, new ToolRegistry(), AgentConfig.defaults(), 1024,
                environment, new EnvironmentReminderFormatter(), null, null, null,
                false, hooks, new HookContextFactory(workspace));
    }

    private static final class CapturingClient implements LlmClient {
        private final boolean fail;
        private ChatRequest request;
        private CapturingClient(boolean fail) { this.fail = fail; }
        @Override public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) throws LlmException {
            this.request = request;
            if (fail) throw new LlmException(LlmErrorType.SERVER_ERROR, false, 500, "provider failed");
            listener.onEvent(new LlmEvent.TextDelta("done"));
            listener.onEvent(new LlmEvent.StreamCompleted(TokenUsage.unknown()));
            return new ChatResponse("done");
        }
        @Override public void close() { }
    }

    private static final class RecordingHooks implements HookRuntime {
        private final List<HookEvent> events = new ArrayList<>();
        private final ArrayDeque<SystemReminder> prompts = new ArrayDeque<>();
        @Override public HookRunResult runHooks(HookContext context) {
            events.add(context.event());
            if (context.event() == HookEvent.TURN_START)
                prompts.add(new SystemReminder(ReminderScope.ROUND, "HOOK_TURN_PROMPT"));
            return HookRunResult.EMPTY;
        }
        @Override public PreToolHookResult runPreToolHooks(HookContext context) { return PreToolHookResult.ALLOW; }
        @Override public List<SystemReminder> drainPrompts() {
            List<SystemReminder> result = List.copyOf(prompts); prompts.clear(); return result;
        }
        @Override public List<HookNotification> drainNotifications() { return List.of(); }
        @Override public void clearPrompts() { prompts.clear(); }
    }
}
