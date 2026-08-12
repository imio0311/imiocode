package io.imiocode.team;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.agent.Agent;
import io.imiocode.agent.AgentRequest;
import io.imiocode.agent.AgentEventListener;
import io.imiocode.config.AgentConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.ConversationRuntimePolicy;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CoordinatorDynamicPolicyTest {
    @Test
    void toolExecutionImmediatelyNarrowsNextAgentIterationAndUpdatesReminder() {
        AtomicBoolean restricted = new AtomicBoolean();
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("enter", () -> restricted.set(true)));
        registry.register(tool("bash", () -> { }));
        SequencedClient client = new SequencedClient(List.of(
                toolResponse(new ToolCall("call-1", "enter", JsonNodeFactory.instance.objectNode())),
                new ChatResponse("done")));
        try (Agent agent = new Agent(client, registry, AgentConfig.defaults())) {
            agent.setRuntimePolicy(new ConversationRuntimePolicy() {
                @Override public Optional<ToolSelection> selection() {
                    return restricted.get() ? Optional.of(ToolSelection.only(Set.of("enter"))) : Optional.empty();
                }
                @Override public List<SystemReminder> reminders() {
                    return restricted.get() ? List.of(new SystemReminder("Coordinator / RESEARCH")) : List.of();
                }
            });
            var result = agent.run(new AgentRequest(new ChatMessage(MessageRole.USER, "coordinate")),
                    AgentEventListener.NOOP);
            assertTrue(result.completed());
        }
        assertEquals(2, client.requests.size());
        assertTrue(client.requests.getFirst().toolSelection().allows("bash"));
        assertFalse(client.requests.getLast().toolSelection().allows("bash"));
        assertTrue(client.requests.getLast().reminders().stream()
                .anyMatch(reminder -> reminder.content().contains("RESEARCH")));
    }

    private static Tool tool(String name, Runnable action) {
        return new Tool() {
            @Override public ToolDefinition definition() {
                return new ToolDefinition(name, "test",
                        JsonNodeFactory.instance.objectNode().put("type", "object"), ToolRisk.LOW);
            }
            @Override public ToolResult execute(ObjectNode arguments) {
                action.run();
                return ToolResult.success("ok");
            }
        };
    }

    private static ChatResponse toolResponse(ToolCall call) {
        return new ChatResponse(new ChatMessage(MessageRole.ASSISTANT,
                List.of(new ToolCallPart(call))));
    }

    private static final class SequencedClient implements LlmClient {
        private final List<ChatResponse> responses;
        private final List<ChatRequest> requests = new ArrayList<>();
        private SequencedClient(List<ChatResponse> responses) { this.responses = responses; }
        @Override public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) {
            requests.add(request);
            ChatResponse response = responses.get(requests.size() - 1);
            if (!response.text().isEmpty()) listener.onEvent(new LlmEvent.TextDelta(response.text()));
            for (int index = 0; index < response.toolCalls().size(); index++) {
                ToolCall call = response.toolCalls().get(index);
                listener.onEvent(new LlmEvent.ToolCallStarted(index, call.id(), call.name()));
                listener.onEvent(new LlmEvent.ToolCallCompleted(index, call));
            }
            listener.onEvent(new LlmEvent.StreamCompleted(response.usage()));
            return response;
        }
        @Override public void close() { }
    }
}
