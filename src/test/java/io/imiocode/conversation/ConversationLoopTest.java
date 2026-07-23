package io.imiocode.conversation;

import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.llm.transport.MockLlmServer;
import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiContext;
import io.imiocode.terminal.UiState;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutor;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationLoopTest {
    @Test
    void ignoresBlankInputStreamsResponseAndExits() {
        FakeClient client = new FakeClient();
        FakeTerminal terminal = new FakeTerminal("", "   ", "hello", "/exit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(1, client.calls);
        assertEquals("hello", client.requests.getFirst().messages().getFirst().content());
        assertEquals(List.of("你", "好"), terminal.deltas);
        assertEquals(1, terminal.beginCount);
        assertEquals(1, terminal.endCount);
        assertEquals(List.of(
                UiState.THINKING,
                UiState.STREAMING,
                UiState.READY), terminal.states);
        assertTrue(client.closed);
    }

    @Test
    void recoversAfterRecoverableFailure() {
        FakeClient client = new FakeClient();
        client.failFirst = true;
        FakeTerminal terminal = new FakeTerminal("first", "second", "/quit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(2, client.calls);
        assertEquals(1, terminal.errors.size());
        assertTrue(terminal.errors.get(0).contains("本轮响应未完成"));
        assertEquals(List.of(
                UiState.THINKING,
                UiState.STREAMING,
                UiState.ERROR,
                UiState.THINKING,
                UiState.STREAMING,
                UiState.READY), terminal.states);
    }

    @Test
    void displaysToolLifecycleThenFinalResponse() {
        ToolCall call = new ToolCall(
                "c1", "read_file",
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                        .put("path", "a.txt"));
        LlmClient client = new LlmClient() {
            private int calls;

            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener) {
                if (calls++ == 0) {
                    return new ChatResponse(new ChatMessage(
                            MessageRole.ASSISTANT, List.of(new ToolCallPart(call))));
                }
                listener.onTextDelta("完成");
                return new ChatResponse("完成");
            }

            @Override
            public void close() {
            }
        };
        ToolRegistry registry = new ToolRegistry();
        registry.register(stubTool());
        FakeTerminal terminal = new FakeTerminal("read", "/exit");

        new ConversationLoop(
                new ConversationSession(client, new ToolExecutor(registry)),
                terminal).run();

        assertEquals(3, terminal.toolEvents.size());
        assertEquals(List.of(
                UiState.THINKING,
                UiState.TOOL_WAITING,
                UiState.TOOL_RUNNING,
                UiState.THINKING,
                UiState.STREAMING,
                UiState.READY), terminal.states);
        assertEquals(List.of("完成"), terminal.deltas);
    }

    @Test
    void applicationProcessCompletesLocalToolRoundTrip() throws Exception {
        try (MockLlmServer server = new MockLlmServer();
             var readerThread = Executors.newVirtualThreadPerTaskExecutor()) {
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"tool_calls\":["
                    + "{\"index\":0,\"id\":\"call_1\",\"function\":{\"name\":\"read_file\","
                    + "\"arguments\":\"{\\\"path\\\":\\\"pom.xml\\\"}\"}}]},"
                    + "\"finish_reason\":\"tool_calls\"}]}\n\n"
                    + "data: [DONE]\n\n");
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"content\":\"项目使用 Java 21。\"},"
                    + "\"finish_reason\":\"stop\"}]}\n\n"
                    + "data: [DONE]\n\n");
            String java = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                    .toString();
            String classpath = System.getProperty(
                    "surefire.test.class.path", System.getProperty("java.class.path"));
            ProcessBuilder builder = new ProcessBuilder(
                    java, "-cp", classpath, "io.imiocode.ImioCodeApplication");
            builder.directory(Path.of("").toAbsolutePath().toFile());
            builder.redirectErrorStream(true);
            builder.environment().put("IMIO_PROVIDER", "deepseek");
            builder.environment().put("IMIO_MODEL", "deepseek-chat");
            builder.environment().put("DEEPSEEK_API_KEY", "local-test-key");
            builder.environment().put("DEEPSEEK_BASE_URL", server.baseUri().toString());
            Process process = builder.start();
            try {
                var outputFuture = readerThread.submit(
                        () -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                try (OutputStreamWriter input =
                             new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                    input.write("请读取 pom.xml，告诉我项目使用的 Java 版本。\n");
                    input.write("/exit\n");
                    input.flush();
                }

                assertTrue(process.waitFor(20, TimeUnit.SECONDS), "应用进程未在时限内退出");
                String output = outputFuture.get(2, TimeUnit.SECONDS);
                assertEquals(0, process.exitValue(), output);
                assertTrue(output.contains("read_file"), output);
                assertTrue(output.contains("LOW"), output);
                assertTrue(output.contains("项目使用 Java 21。"), output);
                assertTrue(server.takeRequest().body().contains("\"tools\""));
                String followUp = server.takeRequest().body();
                assertTrue(followUp.contains("\"role\":\"tool\""));
                assertTrue(followUp.contains("maven.compiler.release"));
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }
    }

    @Test
    void reportsExecutedToolsWhenFinalResponseFails() {
        ToolCall call = new ToolCall(
                "c1", "read_file",
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                        .put("path", "a.txt"));
        LlmClient client = new LlmClient() {
            private int calls;

            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
                if (calls++ == 0) {
                    return new ChatResponse(new ChatMessage(
                            MessageRole.ASSISTANT, List.of(new ToolCallPart(call))));
                }
                throw new LlmException(LlmErrorType.NETWORK, true, null, "最终回复失败");
            }

            @Override
            public void close() {
            }
        };
        ToolRegistry registry = new ToolRegistry();
        registry.register(stubTool());
        FakeTerminal terminal = new FakeTerminal("run", "/quit");

        ConversationSession session = new ConversationSession(client, new ToolExecutor(registry));
        new ConversationLoop(session, terminal).run();

        assertEquals(1, terminal.errors.size());
        assertTrue(terminal.errors.getFirst().contains("工具已经执行"));
        assertEquals(List.of(), session.historySnapshot());
    }

    private static Tool stubTool() {
        return new Tool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(
                        "read_file",
                        "测试",
                        com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                                .put("type", "object"),
                        ToolRisk.LOW);
            }

            @Override
            public ToolResult execute(com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                return ToolResult.success("ok");
            }
        };
    }

    private static final class FakeClient implements LlmClient {
        private int calls;
        private boolean failFirst;
        private boolean closed;
        private final List<ChatRequest> requests = new ArrayList<>();

        @Override
        public ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
            calls++;
            requests.add(request);
            if (failFirst && calls == 1) {
                listener.onTextDelta("部分");
                throw new LlmException(LlmErrorType.NETWORK, true, null, "网络错误");
            }
            listener.onTextDelta("你");
            listener.onTextDelta("好");
            return new ChatResponse("你好");
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class FakeTerminal implements TerminalUi {
        private final Deque<String> inputs;
        private final List<String> deltas = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();
        private final List<UiState> states = new ArrayList<>();
        private final List<ToolExecutionEvent> toolEvents = new ArrayList<>();
        private int beginCount;
        private int endCount;
        private Runnable interruptHandler;
        private UiState state = UiState.READY;

        private FakeTerminal(String... inputs) {
            this.inputs = new ArrayDeque<>(List.of(inputs));
        }

        @Override
        public void showWelcome(UiContext context) {
        }

        @Override
        public void updateState(UiState state) {
            this.state = state;
            states.add(state);
        }

        @Override
        public UiState state() {
            return state;
        }

        @Override
        public String readLine(String prompt) {
            return inputs.isEmpty() ? null : inputs.removeFirst();
        }

        @Override
        public void beginAssistantResponse() {
            beginCount++;
        }

        @Override
        public void appendAssistantText(String text) {
            deltas.add(text);
        }

        @Override
        public void endAssistantResponse() {
            endCount++;
        }

        @Override
        public void showToolEvent(ToolExecutionEvent event) {
            toolEvents.add(event);
        }

        @Override
        public void printError(String message) {
            errors.add(message);
        }

        @Override
        public void printInfo(String message) {
        }

        @Override
        public void setInterruptHandler(Runnable handler) {
            interruptHandler = handler;
        }

        @Override
        public void close() {
        }
    }
}
