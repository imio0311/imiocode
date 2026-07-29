package io.imiocode.agent;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.config.AgentConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.prompt.EnvironmentContext;
import io.imiocode.prompt.EnvironmentReminderFormatter;
import io.imiocode.prompt.GitContext;
import io.imiocode.prompt.GitWorkingTreeState;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTest {
    @Test
    void capturesEnvironmentOncePerTaskAndReusesReminderAcrossIterations() {
        AtomicInteger captures = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("read_file", ToolRisk.LOW, new AtomicInteger()));
        SequencedClient client = new SequencedClient(List.of(
                toolResponse(call("c1", "read_file")),
                new ChatResponse("完成")));

        try (Agent agent = new Agent(
                client,
                registry,
                config(3),
                8_192,
                () -> environment(captures.incrementAndGet()),
                new EnvironmentReminderFormatter())) {
            agent.run(request("读取"), AgentEventListener.NOOP);
        }

        assertEquals(1, captures.get());
        assertEquals(2, client.requests.size());
        String first = environmentReminder(client.requests.get(0));
        String second = environmentReminder(client.requests.get(1));
        assertEquals(first, second);
    }

    @Test
    void refreshesEnvironmentForNextUserTask() {
        AtomicInteger captures = new AtomicInteger();
        SequencedClient client = new SequencedClient(List.of(
                new ChatResponse("第一次"),
                new ChatResponse("第二次")));

        try (Agent agent = new Agent(
                client,
                new ToolRegistry(),
                config(3),
                8_192,
                () -> environment(captures.incrementAndGet()),
                new EnvironmentReminderFormatter())) {
            agent.run(request("任务一"), AgentEventListener.NOOP);
            agent.run(request("任务二"), AgentEventListener.NOOP);
        }

        assertEquals(2, captures.get());
        assertFalse(environmentReminder(client.requests.get(0))
                .equals(environmentReminder(client.requests.get(1))));
    }

    @Test
    void completesSingleTextResponse() {
        SequencedClient client = new SequencedClient(List.of(new ChatResponse("最终回答")));
        List<AgentEvent> events = new ArrayList<>();
        AgentResult result;
        try (Agent agent = new Agent(client, new ToolRegistry(), config(3))) {
            result = agent.run(request("你好"), events::add);
        }

        assertTrue(result.completed());
        assertEquals("最终回答", result.finalResponse().orElseThrow().text());
        assertEquals(List.of(MessageRole.USER, MessageRole.ASSISTANT),
                result.trajectory().stream().map(ChatMessage::role).toList());
        assertEquals(1, events.stream().filter(AgentEvent.TaskCompleted.class::isInstance).count());
    }

    @Test
    void executesToolsAndReturnsResultsToNextIteration() {
        ToolCall call = call("c1", "read_file");
        SequencedClient client = new SequencedClient(List.of(
                toolResponse(call),
                new ChatResponse("读取完成")
        ));
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("read_file", ToolRisk.LOW, new AtomicInteger()));

        AgentResult result;
        try (Agent agent = new Agent(client, registry, config(3))) {
            result = agent.run(request("读取文件"), AgentEventListener.NOOP);
        }

        assertTrue(result.completed());
        assertTrue(result.toolsExecuted());
        assertEquals(2, client.requests.size());
        assertEquals(3, client.requests.get(1).messages().size());
        ChatMessage toolMessage = client.requests.get(1).messages().getLast();
        assertEquals(MessageRole.TOOL, toolMessage.role());
        assertEquals("c1", ((ToolResultPart) toolMessage.parts().getFirst()).callId());
        assertEquals(List.of(
                MessageRole.USER,
                MessageRole.ASSISTANT,
                MessageRole.TOOL,
                MessageRole.ASSISTANT
        ), result.trajectory().stream().map(ChatMessage::role).toList());
    }

    @Test
    void doesNotExecuteToolsRequestedAtMaximumIteration() {
        AtomicInteger runs = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("write_file", ToolRisk.MEDIUM, runs));
        SequencedClient client = new SequencedClient(
                List.of(toolResponse(call("c1", "write_file"))));

        AgentResult result;
        try (Agent agent = new Agent(client, registry, config(1))) {
            result = agent.run(request("修改"), AgentEventListener.NOOP);
        }

        assertEquals(AgentStopReason.MAX_ITERATIONS, result.stopReason());
        assertEquals(0, runs.get());
        assertFalse(result.sideEffectsPossible());
    }

    @Test
    void planModeFiltersToolsAndAddsReminder() {
        AtomicInteger writeRuns = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("read_file", ToolRisk.LOW, new AtomicInteger()));
        registry.register(tool("write_file", ToolRisk.MEDIUM, writeRuns));
        SequencedClient client = new SequencedClient(List.of(
                toolResponse(call("c1", "write_file")),
                new ChatResponse("计划如下")
        ));

        AgentResult result;
        try (Agent agent = new Agent(client, registry, config(3))) {
            agent.switchMode(AgentMode.PLAN, AgentEventListener.NOOP);
            result = agent.run(request("制定计划"), AgentEventListener.NOOP);
        }

        assertTrue(result.completed());
        assertEquals(0, writeRuns.get());
        assertTrue(client.requests.stream()
                .allMatch(request -> request.toolSelection().allowedNames()
                        .equals(PlanModePrompt.READ_ONLY_TOOLS)));
        assertTrue(client.requests.stream()
                .allMatch(request -> request.reminders().stream()
                        .anyMatch(reminder -> reminder.content().contains("Plan Mode"))));
        ToolResultPart resultPart = (ToolResultPart) client.requests.get(1)
                .messages().getLast().parts().getFirst();
        assertFalse(resultPart.result().success());
    }

    @Test
    void listenerFailureStopsTaskAsSafeError() {
        SequencedClient client = new SequencedClient(List.of(new ChatResponse("不会提交")));
        AgentResult result;
        try (Agent agent = new Agent(client, new ToolRegistry(), config(3))) {
            result = agent.run(request("触发监听器异常"), event -> {
                throw new IllegalStateException("终端故障细节");
            });
        }

        assertEquals(AgentStopReason.ERROR, result.stopReason());
        assertEquals("Agent 执行失败", result.error().orElseThrow().safeMessage());
        assertFalse(result.error().orElseThrow().safeMessage().contains("终端故障细节"));
    }

    @Test
    void thirdUnknownToolStopsTaskBeforeFollowingToolRuns() {
        AtomicInteger validRuns = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("valid", ToolRisk.LOW, validRuns));
        ChatResponse response = new ChatResponse(new ChatMessage(
                MessageRole.ASSISTANT,
                List.of(
                        new ToolCallPart(call("u1", "missing1")),
                        new ToolCallPart(call("u2", "missing2")),
                        new ToolCallPart(call("u3", "missing3")),
                        new ToolCallPart(call("v1", "valid")))));
        SequencedClient client = new SequencedClient(List.of(response));
        List<AgentEvent> events = new ArrayList<>();

        AgentResult result;
        try (Agent agent = new Agent(client, registry, config(3))) {
            result = agent.run(request("测试未知工具"), events::add);
        }

        assertEquals(AgentStopReason.TOO_MANY_UNKNOWN_TOOLS, result.stopReason());
        assertEquals(0, validRuns.get());
        assertEquals(1, client.requests.size());
        assertEquals(1, events.stream()
                .filter(AgentEvent.TaskStopped.class::isInstance).count());
    }

    private static AgentConfig config(int maxIterations) {
        return new AgentConfig(maxIterations, Duration.ofSeconds(5), 2);
    }

    private static EnvironmentContext environment(int sequence) {
        return new EnvironmentContext(
                Path.of("D:/workspace"),
                "Windows",
                "PowerShell",
                ZonedDateTime.of(2026, 7, 29, 10, sequence, 0, 0,
                        ZoneId.of("Asia/Shanghai")),
                new GitContext(
                        Optional.of("ch5"),
                        GitWorkingTreeState.CLEAN));
    }

    private static String environmentReminder(ChatRequest request) {
        return request.reminders().stream()
                .filter(reminder -> reminder.scope() == ReminderScope.ENVIRONMENT)
                .findFirst()
                .orElseThrow()
                .content();
    }

    private static AgentRequest request(String text) {
        return new AgentRequest(new ChatMessage(MessageRole.USER, text));
    }

    private static ChatResponse toolResponse(ToolCall call) {
        return new ChatResponse(new ChatMessage(
                MessageRole.ASSISTANT,
                List.of(new ToolCallPart(call))
        ));
    }

    private static ToolCall call(String id, String name) {
        return new ToolCall(id, name, JsonNodeFactory.instance.objectNode());
    }

    private static Tool tool(String name, ToolRisk risk, AtomicInteger runs) {
        return new Tool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(
                        name,
                        "测试工具",
                        JsonNodeFactory.instance.objectNode().put("type", "object"),
                        risk
                );
            }

            @Override
            public ToolResult execute(ObjectNode arguments) {
                runs.incrementAndGet();
                return ToolResult.success(name);
            }
        };
    }

    private static final class SequencedClient implements LlmClient {
        private final List<ChatResponse> responses;
        private final List<ChatRequest> requests = new ArrayList<>();

        private SequencedClient(List<ChatResponse> responses) {
            this.responses = responses;
        }

        @Override
        public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) {
            requests.add(request);
            ChatResponse response = responses.get(requests.size() - 1);
            if (!response.text().isEmpty()) {
                listener.onEvent(new LlmEvent.TextDelta(response.text()));
            }
            for (int index = 0; index < response.toolCalls().size(); index++) {
                ToolCall call = response.toolCalls().get(index);
                listener.onEvent(new LlmEvent.ToolCallStarted(
                        index, call.id(), call.name()));
                listener.onEvent(new LlmEvent.ToolCallCompleted(index, call));
            }
            listener.onEvent(new LlmEvent.StreamCompleted(response.usage()));
            return response;
        }

        @Override
        public void close() {
        }
    }
}
