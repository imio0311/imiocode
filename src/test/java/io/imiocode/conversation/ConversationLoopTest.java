package io.imiocode.conversation;

import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentEvent;
import io.imiocode.agent.PlanModePrompt;
import io.imiocode.config.UiVerbosity;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmEvent;
import io.imiocode.llm.LlmEventListener;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.llm.TokenUsage;
import io.imiocode.llm.TokenUsageBuilder;
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
import org.junit.jupiter.api.io.TempDir;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationLoopTest {
    @TempDir
    Path tempDirectory;

    @Test
    void compactCommandIsHandledLocallyAndRestoresReadyState() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient client = new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener) {
                calls.incrementAndGet();
                return new ChatResponse("不应调用");
            }
            @Override public void close() { }
        };
        FakeTerminal terminal = new FakeTerminal("/CoMpAcT", "/exit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(0, calls.get());
        assertEquals(List.of(UiState.COMPACTING, UiState.READY), terminal.states);
    }

    @Test
    void uiVerbosityCommandsAreHandledLocallyWithoutChangingAgentMode() {
        FakeClient client = new FakeClient();
        FakeTerminal terminal = new FakeTerminal(
                "/VeRbOsE", "/compact-ui", "/verbose", "/exit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(0, client.calls);
        assertEquals(List.of(
                UiVerbosity.VERBOSE,
                UiVerbosity.COMPACT,
                UiVerbosity.VERBOSE), terminal.verbosityChanges);
        assertEquals(UiVerbosity.VERBOSE, terminal.verbosity());
        assertEquals(List.of(), terminal.agentModes);
    }

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

        assertEquals(3, client.calls);
        assertEquals(0, terminal.errors.size());
        assertEquals(1, terminal.retries.size());
        assertEquals(List.of(
                UiState.THINKING,
                UiState.STREAMING,
                UiState.THINKING,
                UiState.STREAMING,
                UiState.READY,
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
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"先查看项目\","
                    + "\"tool_calls\":["
                    + "{\"index\":0,\"id\":\"call_1\",\"function\":{\"name\":\"read_file\","
                    + "\"arguments\":\"{\\\"path\\\":\\\"pom.xml\\\"}\"}}]},"
                    + "\"finish_reason\":\"tool_calls\"}],"
                    + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":4,"
                    + "\"completion_tokens_details\":{\"reasoning_tokens\":2}}}\n\n"
                    + "data: [DONE]\n\n");
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"总结结果\","
                    + "\"content\":\"项目使用 Java 21。\"},\"finish_reason\":\"stop\"}],"
                    + "\"usage\":{\"prompt_tokens\":20,\"completion_tokens\":6,"
                    + "\"completion_tokens_details\":{\"reasoning_tokens\":2}}}\n\n"
                    + "data: [DONE]\n\n");
            String java = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                    .toString();
            String classpath = System.getProperty(
                    "surefire.test.class.path", System.getProperty("java.class.path"));
            ProcessBuilder builder = new ProcessBuilder(
                    java, "-cp", classpath, "io.imiocode.ImioCodeApplication");
            Files.copy(Path.of("pom.xml").toAbsolutePath(), tempDirectory.resolve("pom.xml"));
            builder.directory(tempDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.environment().put("IMIO_PROVIDER", "deepseek");
            builder.environment().put("IMIO_MODEL", "deepseek-chat");
            builder.environment().put("DEEPSEEK_API_KEY", "local-test-key");
            builder.environment().put("DEEPSEEK_BASE_URL", server.baseUri().toString());
            builder.environment().put("IMIO_THINKING_ENABLED", "true");
            builder.environment().put("IMIO_REASONING_EFFORT", "medium");
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
                assertTrue(output.contains("[ok] Read pom.xml"), output);
                assertTrue(!output.contains("LOW"), output);
                assertTrue(!output.contains("[thinking]"), output);
                assertTrue(!output.contains("[usage]"), output);
                assertTrue(output.contains("项目使用 Java 21。"), output);
                String firstRequest = server.takeRequest().body();
                assertTrue(firstRequest.contains("\"tools\""));
                assertTrue(firstRequest.contains("\"thinking\":{\"type\":\"enabled\"}"));
                assertTrue(firstRequest.contains("\"reasoning_effort\":\"medium\""));
                String followUp = server.takeRequest().body();
                assertTrue(followUp.contains("\"role\":\"tool\""));
                assertTrue(followUp.contains("maven.compiler.release"));
                assertTrue(followUp.contains("\"reasoning_content\":\"先查看项目\""));
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        }
    }

    @Test
    void applicationProcessSwitchesUiVerbosityWithoutExtraModelTurns() throws Exception {
        try (MockLlmServer server = new MockLlmServer();
             var readerThread = Executors.newVirtualThreadPerTaskExecutor()) {
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"reasoning_content\":\"inspect\","
                    + "\"tool_calls\":["
                    + "{\"index\":0,\"id\":\"call_1\",\"function\":{\"name\":\"read_file\","
                    + "\"arguments\":\"{\\\"path\\\":\\\"pom.xml\\\"}\"}}]},"
                    + "\"finish_reason\":\"tool_calls\"}],"
                    + "\"usage\":{\"prompt_tokens\":7,\"completion_tokens\":3}}\n\n"
                    + "data: [DONE]\n\n");
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"content\":\"done\"},"
                    + "\"finish_reason\":\"stop\"}],"
                    + "\"usage\":{\"prompt_tokens\":11,\"completion_tokens\":2}}\n\n"
                    + "data: [DONE]\n\n");
            String java = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                    .toString();
            String classpath = System.getProperty(
                    "surefire.test.class.path", System.getProperty("java.class.path"));
            ProcessBuilder builder = new ProcessBuilder(
                    java, "-cp", classpath, "io.imiocode.ImioCodeApplication");
            Files.copy(Path.of("pom.xml").toAbsolutePath(), tempDirectory.resolve("pom.xml"));
            builder.directory(tempDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.environment().put("IMIO_PROVIDER", "deepseek");
            builder.environment().put("IMIO_MODEL", "deepseek-chat");
            builder.environment().put("DEEPSEEK_API_KEY", "local-test-key");
            builder.environment().put("DEEPSEEK_BASE_URL", server.baseUri().toString());
            builder.environment().put("IMIO_THINKING_ENABLED", "true");
            Process process = builder.start();
            try {
                var outputFuture = readerThread.submit(
                        () -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                try (OutputStreamWriter input =
                             new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                    input.write("/verbose\n");
                    input.write("read pom.xml\n");
                    input.write("/compact-ui\n");
                    input.write("/exit\n");
                    input.flush();
                }

                assertTrue(process.waitFor(20, TimeUnit.SECONDS), "应用进程未在时限内退出");
                String output = outputFuture.get(2, TimeUnit.SECONDS);
                assertEquals(0, process.exitValue(), output);
                assertTrue(output.contains("[UI] 详细模式"), output);
                assertTrue(output.contains("[thinking] inspect"), output);
                assertTrue(output.contains("[usage] input=7 · output=3"), output);
                assertTrue(output.contains("read_file"), output);
                assertTrue(output.contains("LOW"), output);
                assertTrue(output.contains("queued"), output);
                assertTrue(output.contains("running"), output);
                assertTrue(output.contains("done"), output);
                assertTrue(output.contains("[UI] 精简模式"), output);
                server.takeRequest();
                server.takeRequest();
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

    @Test
    void routesThinkingTextAndUsageWithoutShowingMetadata() {
        LlmClient client = new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, LlmEventListener listener) {
                ThinkingPart thinking = new ThinkingPart(
                        "分析",
                        new OpenAiReasoningMetadata("reasoning-secret-id", "encrypted-secret"));
                TokenUsage usage = new TokenUsageBuilder().input(8).output(3).reasoning(2).build();
                listener.onEvent(new LlmEvent.ThinkingDelta(0, "分析"));
                listener.onEvent(new LlmEvent.ThinkingCompleted(0, thinking));
                listener.onEvent(new LlmEvent.TextDelta("答案"));
                listener.onEvent(new LlmEvent.StreamCompleted(usage));
                return new ChatResponse(
                        new ChatMessage(
                                MessageRole.ASSISTANT,
                                List.of(thinking, new TextPart("答案"))),
                        usage);
            }

            @Override
            public void close() {
            }
        };
        FakeTerminal terminal = new FakeTerminal("think", "/exit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(List.of("分析"), terminal.thinkingDeltas);
        assertEquals(List.of("答案"), terminal.deltas);
        assertEquals(1, terminal.usages.size());
        assertEquals(OptionalLong.of(2), terminal.usages.getFirst().reasoningTokens());
        assertEquals(
                List.of("thinking-begin", "thinking:分析", "thinking-end",
                        "answer-begin", "answer:答案", "answer-end", "usage"),
                terminal.richActions);
        String visible = String.join("|", terminal.richActions);
        assertTrue(!visible.contains("reasoning-secret-id"));
        assertTrue(!visible.contains("encrypted-secret"));
    }

    @Test
    void automaticallyRetriesRateLimitAndDisplaysRetries() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient client = new LlmClient() {
            @Override
            public ChatResponse streamChat(ChatRequest request, StreamListener listener)
                    throws LlmException {
                calls.incrementAndGet();
                throw new LlmException(
                        LlmErrorType.RATE_LIMIT,
                        true,
                        429,
                        "请求过于频繁",
                        Duration.ZERO,
                        null);
            }

            @Override
            public void close() {
            }
        };
        FakeTerminal terminal = new FakeTerminal("hello", "/exit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(4, calls.get());
        assertEquals(3, terminal.retries.size());
        assertEquals(1, terminal.errors.size());
        assertTrue(terminal.errors.getFirst().contains("本轮响应未完成"));
    }

    @Test
    void planAndDoCommandsSwitchToolsWithoutModelRequests() {
        FakeClient client = new FakeClient();
        FakeTerminal terminal = new FakeTerminal(
                "/plan", "先规划", "/do", "再执行", "/exit");

        new ConversationLoop(new ConversationSession(client), terminal).run();

        assertEquals(2, client.calls);
        assertEquals(List.of(AgentMode.PLAN, AgentMode.DO), terminal.agentModes);
        assertEquals(PlanModePrompt.READ_ONLY_TOOLS,
                client.requests.getFirst().toolSelection().allowedNames());
        assertTrue(client.requests.get(1).toolSelection().unrestricted());
        assertTrue(client.requests.getFirst().reminders().stream()
                .anyMatch(reminder -> reminder.content().contains("Plan Mode")));
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
        private final List<String> thinkingDeltas = new ArrayList<>();
        private final List<TokenUsage> usages = new ArrayList<>();
        private final List<String> richActions = new ArrayList<>();
        private final List<AgentMode> agentModes = new ArrayList<>();
        private final List<UiVerbosity> verbosityChanges = new ArrayList<>();
        private final List<AgentEvent.RetryScheduled> retries = new ArrayList<>();
        private int beginCount;
        private int endCount;
        private boolean answerOpen;
        private boolean thinkingOpen;
        private Runnable interruptHandler;
        private UiState state = UiState.READY;
        private UiVerbosity verbosity = UiVerbosity.COMPACT;

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
        public UiVerbosity verbosity() {
            return verbosity;
        }

        @Override
        public void setVerbosity(UiVerbosity verbosity) {
            this.verbosity = verbosity;
        }

        @Override
        public void showVerbosityChanged(UiVerbosity verbosity) {
            verbosityChanges.add(verbosity);
        }

        @Override
        public String readLine(String prompt) {
            return inputs.isEmpty() ? null : inputs.removeFirst();
        }

        @Override
        public void beginAssistantResponse() {
            beginCount++;
            if (!answerOpen) {
                answerOpen = true;
                richActions.add("answer-begin");
            }
        }

        @Override
        public void appendAssistantText(String text) {
            deltas.add(text);
            richActions.add("answer:" + text);
        }

        @Override
        public void endAssistantResponse() {
            endCount++;
            if (answerOpen) {
                answerOpen = false;
                richActions.add("answer-end");
            }
        }

        @Override
        public void beginThinking() {
            if (!thinkingOpen) {
                thinkingOpen = true;
                richActions.add("thinking-begin");
            }
        }

        @Override
        public void appendThinkingText(String text) {
            thinkingDeltas.add(text);
            richActions.add("thinking:" + text);
        }

        @Override
        public void endThinking() {
            if (thinkingOpen) {
                thinkingOpen = false;
                richActions.add("thinking-end");
            }
        }

        @Override
        public void showUsage(TokenUsage usage) {
            usages.add(usage);
            richActions.add("usage");
        }

        @Override
        public void showToolEvent(ToolExecutionEvent event) {
            toolEvents.add(event);
        }

        @Override
        public void showAgentMode(AgentMode mode) {
            agentModes.add(mode);
        }

        @Override
        public void showRetry(AgentEvent.RetryScheduled retry) {
            retries.add(retry);
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
