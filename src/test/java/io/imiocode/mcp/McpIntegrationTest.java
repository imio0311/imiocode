package io.imiocode.mcp;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.agent.Agent;
import io.imiocode.agent.AgentEventListener;
import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentRequest;
import io.imiocode.agent.AgentResult;
import io.imiocode.config.AgentConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.mcp.config.McpTransportType;
import io.imiocode.mcp.config.ResolvedMcpServerConfig;
import io.imiocode.mcp.fixture.FakeStdioMcpServer;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.manager.McpManager;
import io.imiocode.mcp.manager.McpStartupResult;
import io.imiocode.mcp.transport.McpTransportFactory;
import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionRequest;
import io.imiocode.permission.PermissionRequestFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpIntegrationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void registersRealStdioToolAndExecutesItThroughAgentLoop() {
        ToolRegistry registry = new ToolRegistry();
        SecretRedactor redactor = new SecretRedactor("");
        McpManager manager = new McpManager(
                new McpTransportFactory(new JsonRpcCodec()),
                request -> true,
                event -> { },
                ToolLimits.defaults(),
                redactor,
                "test");
        McpStartupResult startup = manager.start(List.of(config("fake", "normal")), registry);
        String toolName = "mcp_fake__echo_text";
        assertEquals(1, startup.connectedServers(), startup.errors().toString());
        assertEquals(1, startup.registeredTools());
        assertTrue(registry.enabledNames().contains(toolName));

        ToolCall call = new ToolCall(
                "mcp-call-1",
                toolName,
                JsonNodeFactory.instance.objectNode().put("text", "来自 Agent 的中文"));
        SequencedClient llm = new SequencedClient(List.of(
                new ChatResponse(new ChatMessage(
                        MessageRole.ASSISTANT,
                        List.of(new ToolCallPart(call)))),
                new ChatResponse("远程工具执行完成")));
        AgentResult result;
        try (Agent agent = new Agent(
                llm,
                registry,
                new AgentConfig(3, Duration.ofSeconds(10), 2))) {
            result = agent.run(
                    new AgentRequest(new ChatMessage(MessageRole.USER, "调用远程回显工具")),
                    AgentEventListener.NOOP);
        }

        assertTrue(result.completed());
        ToolResultPart toolResult = (ToolResultPart) llm.requests.get(1)
                .messages().getLast().parts().getFirst();
        assertEquals("来自 Agent 的中文", toolResult.result().output());
        assertFalse(allRequestText(llm.requests).contains("MALICIOUS_INSTRUCTIONS_SENTINEL"));

        PermissionRequest permission = new PermissionRequestFactory(redactor).create(
                call,
                registry.findEnabled(toolName).orElseThrow().definition());
        assertEquals(PermissionOperation.COMMAND, permission.operation());
        assertEquals(toolName, permission.displayTarget());

        manager.close();
        assertTrue(manager.activeClients().isEmpty());
    }

    @Test
    void planModeDoesNotExposeMcpTool() {
        ToolRegistry registry = new ToolRegistry();
        SecretRedactor redactor = new SecretRedactor("");
        McpManager manager = new McpManager(
                new McpTransportFactory(new JsonRpcCodec()),
                request -> true,
                event -> { },
                ToolLimits.defaults(),
                redactor,
                "test");
        manager.start(List.of(config("fake", "normal")), registry);
        SequencedClient llm = new SequencedClient(List.of(new ChatResponse("计划完成")));

        try (Agent agent = new Agent(
                llm,
                registry,
                new AgentConfig(2, Duration.ofSeconds(10), 2))) {
            agent.switchMode(AgentMode.PLAN, AgentEventListener.NOOP);
            agent.run(
                    new AgentRequest(new ChatMessage(MessageRole.USER, "只制定计划")),
                    AgentEventListener.NOOP);
        }

        assertFalse(llm.requests.getFirst().toolSelection().allows("mcp_fake__echo_text"));
        manager.close();
    }

    @Test
    void malformedStdioServerDoesNotBlockHealthyServer() {
        ToolRegistry registry = new ToolRegistry();
        SecretRedactor redactor = new SecretRedactor("");
        McpManager manager = new McpManager(
                new McpTransportFactory(new JsonRpcCodec()),
                request -> true,
                event -> { },
                ToolLimits.defaults(),
                redactor,
                "test");

        McpStartupResult result = manager.start(
                List.of(config("broken", "malformed"), config("healthy", "normal")),
                registry);

        assertEquals(1, result.connectedServers());
        assertEquals(1, result.errors().size());
        assertTrue(registry.enabledNames().contains("mcp_healthy__echo_text"));
        manager.close();
    }

    private ResolvedMcpServerConfig config(String name, String mode) {
        String javaExecutable = Path.of(
                System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
        return new ResolvedMcpServerConfig(
                name,
                McpTransportType.STDIO,
                javaExecutable,
                List.of("-cp", System.getProperty("java.class.path"),
                        FakeStdioMcpServer.class.getName(), mode),
                null,
                Map.of(),
                Map.of(),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                tempDirectory.resolve(".imiocode/mcp.yaml"));
    }

    private static String allRequestText(List<ChatRequest> requests) {
        StringBuilder text = new StringBuilder();
        requests.forEach(request -> {
            request.messages().forEach(message -> text.append(message.content()));
            request.reminders().forEach(reminder -> text.append(reminder.content()));
        });
        return text.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win");
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
                listener.onEvent(new LlmEvent.ToolCallStarted(index, call.id(), call.name()));
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
