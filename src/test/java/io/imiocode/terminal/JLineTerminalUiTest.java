package io.imiocode.terminal;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.config.UiVerbosity;
import io.imiocode.agent.AgentEvent;
import io.imiocode.agent.AgentStopReason;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.mcp.manager.McpEvent;
import io.imiocode.mcp.manager.McpEventType;
import io.imiocode.mcp.manager.McpLaunchRequest;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.tool.ToolResult;
import io.imiocode.llm.TokenUsageBuilder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JLineTerminalUiTest {
    @Test
    void rendersRetryAndUnknownToolStopInPlainMode() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(new byte[0]), output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal, UiVerbosity.VERBOSE);

        ui.beginAssistantResponse();
        ui.appendAssistantText("半截");
        ui.showRetry(new AgentEvent.RetryScheduled(
                1, 2, LlmErrorType.NETWORK, Duration.ofSeconds(1), 16_000));
        ui.showAgentStop(AgentStopReason.TOO_MANY_UNKNOWN_TOOLS, false);
        String text = output.toString(StandardCharsets.UTF_8);

        assertTrue(text.contains("半截"));
        assertTrue(text.contains("[重试]"));
        assertTrue(text.contains("第 2 次尝试"));
        assertTrue(text.contains("连续请求不存在的工具"));
        ui.close();
    }

    @Test
    void readsUtf8AndFormatsAssistantAndErrors() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream("你好\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(input, output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        JLineTerminalUi ui = new JLineTerminalUi(
                terminal, reader, new io.imiocode.tool.SecretRedactor(""), UiVerbosity.VERBOSE);

        assertEquals("你好", ui.readLine("You> "));
        ui.beginAssistantResponse();
        ui.appendAssistantText("答");
        ui.appendAssistantText("案");
        ui.endAssistantResponse();
        ui.printError("失败");
        ui.close();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("ImioCode> 答案"), () -> "实际输出: " + text);
        assertTrue(text.contains("[错误] 失败"));
        assertDoesNotThrow(ui::close);
    }

    @Test
    void rendersPlainWelcomeAndStateWithoutAnsi() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(new byte[0]), output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal, UiVerbosity.VERBOSE);

        ui.showWelcome(new UiContext("ImioCode", "dev", "deepseek", "deepseek-chat", Path.of("work")));
        ui.updateState(UiState.THINKING);
        ui.updateState(UiState.STREAMING);
        ToolCall call = new ToolCall(
                "c1", "read_file", JsonNodeFactory.instance.objectNode().put("path", "a.txt"));
        ui.showToolEvent(new ToolExecutionEvent(ToolExecutionState.QUEUED, call, null));
        ui.showToolEvent(new ToolExecutionEvent(
                ToolExecutionState.RUNNING, call, null));
        ui.showToolEvent(new ToolExecutionEvent(
                ToolExecutionState.SUCCEEDED, call, ToolResult.success("ok")));
        ui.updateState(UiState.READY);
        ui.close();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("ImioCode vdev"));
        assertTrue(text.contains("deepseek | deepseek-chat"), () -> "实际输出: " + text);
        assertTrue(text.contains("Thinking…"));
        assertTrue(text.contains("Streaming"));
        assertTrue(text.contains("read_file"));
        assertTrue(text.contains("LOW"));
        assertTrue(text.contains("成功"));
        assertTrue(text.contains("Ready"));
        assertTrue(!text.contains("\u001B["));
    }

    @Test
    void rendersPlainThinkingAndKnownUsageWithoutSensitiveMetadata() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(new byte[0]), output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal, UiVerbosity.VERBOSE);

        ui.beginThinking();
        ui.appendThinkingText("正在分析");
        ui.endThinking();
        ui.beginAssistantResponse();
        ui.appendAssistantText("最终答案");
        ui.endAssistantResponse();
        ui.showUsage(new TokenUsageBuilder().input(8).output(3).build());
        ui.close();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("[thinking] 正在分析"), () -> "实际输出: " + text);
        assertTrue(text.contains("ImioCode> 最终答案"), () -> "实际输出: " + text);
        assertTrue(text.contains("[usage] input=8 · output=3"), () -> "实际输出: " + text);
        assertTrue(!text.contains("signature-secret"));
        assertTrue(!text.contains("encrypted-secret"));
        assertTrue(!text.contains("\u001B["));
    }

    @Test
    void multilineWidgetInsertsNewlineWithoutUiText() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal, reader);

        reader.getBuffer().write("第一行");
        assertTrue(reader.getWidgets().get("insert-newline").apply());
        reader.getBuffer().write("second line");

        assertEquals("第一行\nsecond line", reader.getBuffer().toString());
        ui.close();
    }

    @Test
    void altEnterProducesOneMultilineMessage() throws Exception {
        byte[] keys = "first\u001b\rsecond\n".getBytes(StandardCharsets.UTF_8);
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(keys), new ByteArrayOutputStream())
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal);

        assertEquals("first\nsecond", ui.readLine("You> "));
        ui.close();
    }

    @Test
    void dispatchesInterruptHandler() throws Exception {
        Terminal terminal = TerminalBuilder.builder().dumb(true).build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal);
        AtomicInteger interrupts = new AtomicInteger();
        ui.setInterruptHandler(interrupts::incrementAndGet);

        terminal.raise(Terminal.Signal.INT);

        assertEquals(1, interrupts.get());
        ui.close();
    }

    @Test
    void confirmsStdioLaunchAndRendersMcpEventsWithoutEnvironmentValues() throws Exception {
        ByteArrayInputStream input = new ByteArrayInputStream("1\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(input, output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal, UiVerbosity.VERBOSE);

        assertTrue(ui.approve(new McpLaunchRequest(
                "github", "java", java.util.List.of("-jar", "server.jar"))));
        ui.onMcpEvent(new McpEvent(
                McpEventType.CONNECTED, "github", "", "已连接，发现 2 个工具"));
        ui.onMcpEvent(new McpEvent(
                McpEventType.TOOL_DISCOVERED, "github", "mcp_github__issues", "工具已注册"));
        ui.close();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("[MCP 启动确认] Server=github"));
        assertTrue(text.contains("命令: java"));
        assertTrue(text.contains("已连接，发现 2 个工具"));
        assertTrue(text.contains("mcp_github__issues"));
        assertTrue(!text.contains("TOKEN_VALUE_SENTINEL"));
    }

    @Test
    void compactModeHidesProcessNoiseAndPrintsOneTerminalToolLine() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(new byte[0]), output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal, UiVerbosity.COMPACT);
        ToolCall call = new ToolCall(
                "c1", "read_file", JsonNodeFactory.instance.objectNode().put("path", "a.txt"));

        ui.showWelcome(new UiContext(
                "ImioCode", "dev", "deepseek", "deepseek-chat", Path.of("work")));
        ui.updateState(UiState.THINKING);
        ui.beginThinking();
        ui.appendThinkingText("hidden thinking");
        ui.endThinking();
        ui.showUsage(new TokenUsageBuilder().input(8).output(3).build());
        ui.showToolEvent(new ToolExecutionEvent(ToolExecutionState.QUEUED, call, null));
        ui.showToolEvent(new ToolExecutionEvent(ToolExecutionState.RUNNING, call, null));
        ui.showToolEvent(new ToolExecutionEvent(
                ToolExecutionState.SUCCEEDED,
                call,
                ToolResult.success("file body").withDuration(Duration.ofMillis(125))));
        ui.updateState(UiState.READY);
        ui.close();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("ImioCode vdev"), () -> "实际输出: " + text);
        assertTrue(text.contains("deepseek | deepseek-chat"), () -> "实际输出: " + text);
        assertTrue(text.contains("目录:"), () -> "实际输出: " + text);
        assertTrue(text.contains("状态: Ready"), () -> "实际输出: " + text);
        assertTrue(!text.contains("状态: Thinking"));
        assertTrue(text.contains("[ok] Read a.txt (0.1s)"), () -> "实际输出: " + text);
        assertTrue(!text.contains("hidden thinking"));
        assertTrue(!text.contains("[usage]"));
        assertTrue(!text.contains("queued"));
        assertTrue(!text.contains("running"));
        assertEquals(UiState.READY, ui.state());
    }

    @Test
    void compactModePrintsOnlyOneRedactedFailureLine() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(new byte[0]), output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(
                terminal, new io.imiocode.tool.SecretRedactor("secret"), UiVerbosity.COMPACT);
        ToolCall call = new ToolCall(
                "call-secret", "bash",
                JsonNodeFactory.instance.objectNode().put("command", "run secret"));

        ui.showToolEvent(new ToolExecutionEvent(ToolExecutionState.QUEUED, call, null));
        ui.showToolEvent(new ToolExecutionEvent(ToolExecutionState.RUNNING, call, null));
        ui.showToolEvent(new ToolExecutionEvent(
                ToolExecutionState.FAILED,
                call,
                ToolResult.failure("first secret\nsecond line")
                        .withDuration(Duration.ofMillis(500))));
        ui.close();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("[fail] Bash run *** · first *** (0.5s)"),
                () -> "实际输出: " + text);
        assertTrue(!text.contains("second line"));
        assertTrue(!text.contains("call-secret"));
        assertEquals(1, text.lines().filter(line -> line.contains("[fail]")).count());
    }

    @Test
    void switchingToVerboseAffectsOnlyFutureEvents() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(new byte[0]), output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal, UiVerbosity.COMPACT);

        ui.beginThinking();
        ui.appendThinkingText("hidden thinking");
        ui.endThinking();
        ui.setVerbosity(UiVerbosity.VERBOSE);
        ui.showVerbosityChanged(UiVerbosity.VERBOSE);
        ui.beginThinking();
        ui.appendThinkingText("visible thinking");
        ui.endThinking();
        ui.showUsage(new TokenUsageBuilder().input(3).output(2).build());
        ui.close();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(!text.contains("hidden thinking"));
        assertTrue(text.contains("[UI] 详细模式"), () -> "实际输出: " + text);
        assertTrue(text.contains("[thinking] visible thinking"), () -> "实际输出: " + text);
        assertTrue(text.contains("[usage] input=3"), () -> "实际输出: " + text);
    }

    @Test
    void compactModeOnlyShowsActionableMcpEvents() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(new byte[0]), output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal, UiVerbosity.COMPACT);

        ui.onMcpEvent(new McpEvent(McpEventType.CONNECTING, "context7", "", "connecting"));
        ui.onMcpEvent(new McpEvent(
                McpEventType.TOOL_DISCOVERED, "context7", "mcp_context7__search", "registered"));
        ui.onMcpEvent(new McpEvent(McpEventType.DENIED, "blocked", "", "denied"));
        ui.onMcpEvent(new McpEvent(McpEventType.SERVER_FAILED, "broken", "", "safe failure"));
        ui.close();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(!text.contains("context7"), () -> "实际输出: " + text);
        assertTrue(text.contains("blocked"), () -> "实际输出: " + text);
        assertTrue(text.contains("broken"), () -> "实际输出: " + text);
        assertTrue(text.contains("safe failure"), () -> "实际输出: " + text);
    }
}
