package io.imiocode.terminal;

import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentEvent;
import io.imiocode.agent.AgentStopReason;
import io.imiocode.permission.PermissionPrompt;
import io.imiocode.permission.PermissionReply;
import io.imiocode.mcp.manager.McpEvent;
import io.imiocode.mcp.manager.McpLaunchRequest;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.context.CompactReport;
import io.imiocode.context.ContextEvent;
import io.imiocode.config.UiVerbosity;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class JLineTerminalUi implements TerminalUi {
    private final Terminal terminal;
    private final LineReader lineReader;
    private final PrintWriter writer;
    private final TerminalLayout layout = new TerminalLayout();
    private final ToolSummaryFormatter toolFormatter;
    private final UsageFormatter usageFormatter = new UsageFormatter();
    private final AtomicReference<Runnable> interruptHandler = new AtomicReference<>(() -> { });
    private final AtomicReference<UiState> state = new AtomicReference<>(UiState.READY);
    private final AtomicReference<UiVerbosity> verbosity;
    private final AtomicBoolean closed = new AtomicBoolean();
    private UiContext context;
    private boolean assistantLineOpen;
    private boolean thinkingLineOpen;

    public JLineTerminalUi() throws IOException {
        this(TerminalBuilder.builder().system(true).encoding(java.nio.charset.StandardCharsets.UTF_8).build(),
                new SecretRedactor(""), UiVerbosity.COMPACT);
    }

    public JLineTerminalUi(SecretRedactor redactor) throws IOException {
        this(TerminalBuilder.builder().system(true).encoding(java.nio.charset.StandardCharsets.UTF_8).build(),
                redactor, UiVerbosity.COMPACT);
    }

    public JLineTerminalUi(SecretRedactor redactor, UiVerbosity verbosity) throws IOException {
        this(TerminalBuilder.builder().system(true).encoding(java.nio.charset.StandardCharsets.UTF_8).build(),
                redactor, verbosity);
    }

    JLineTerminalUi(Terminal terminal) {
        this(terminal, LineReaderBuilder.builder().terminal(terminal).build(),
                new SecretRedactor(""), UiVerbosity.COMPACT);
    }

    JLineTerminalUi(Terminal terminal, UiVerbosity verbosity) {
        this(terminal, LineReaderBuilder.builder().terminal(terminal).build(),
                new SecretRedactor(""), verbosity);
    }

    JLineTerminalUi(Terminal terminal, SecretRedactor redactor) {
        this(terminal, LineReaderBuilder.builder().terminal(terminal).build(),
                redactor, UiVerbosity.COMPACT);
    }

    JLineTerminalUi(Terminal terminal, SecretRedactor redactor, UiVerbosity verbosity) {
        this(terminal, LineReaderBuilder.builder().terminal(terminal).build(), redactor, verbosity);
    }

    JLineTerminalUi(Terminal terminal, LineReader lineReader) {
        this(terminal, lineReader, new SecretRedactor(""), UiVerbosity.COMPACT);
    }

    JLineTerminalUi(Terminal terminal, LineReader lineReader, SecretRedactor redactor) {
        this(terminal, lineReader, redactor, UiVerbosity.COMPACT);
    }

    JLineTerminalUi(
            Terminal terminal,
            LineReader lineReader,
            SecretRedactor redactor,
            UiVerbosity verbosity) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.lineReader = Objects.requireNonNull(lineReader, "lineReader");
        this.writer = terminal.writer();
        this.toolFormatter = new ToolSummaryFormatter(redactor);
        this.verbosity = new AtomicReference<>(Objects.requireNonNull(verbosity, "verbosity"));
        terminal.handle(Terminal.Signal.INT, signal -> interruptHandler.get().run());
        installMultilineWidget();
    }

    @Override
    public synchronized void showWelcome(UiContext context) {
        if (closed.get()) {
            return;
        }
        this.context = Objects.requireNonNull(context, "context");
        TerminalMode mode = currentMode();
        for (String line : layout.welcome(
                context, state.get(), terminalWidth(), mode)) {
            printStyled(line, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN), mode);
        }
        writer.flush();
    }

    @Override
    public synchronized void updateState(UiState state) {
        UiState next = Objects.requireNonNull(state, "state");
        this.state.set(next);
        if (closed.get() || context == null) {
            return;
        }
        if (!displayPolicy().showStateTransitions()) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        TerminalMode mode = currentMode();
        printStyled(layout.statusLine(context, next, terminalWidth(), mode), stateStyle(next), mode);
        writer.flush();
    }

    @Override
    public UiState state() {
        return state.get();
    }

    @Override
    public UiVerbosity verbosity() {
        return verbosity.get();
    }

    @Override
    public synchronized void setVerbosity(UiVerbosity verbosity) {
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        this.verbosity.set(Objects.requireNonNull(verbosity, "verbosity"));
    }

    @Override
    public synchronized void showVerbosityChanged(UiVerbosity verbosity) {
        if (closed.get()) {
            return;
        }
        String line = verbosity == UiVerbosity.VERBOSE
                ? "[UI] 详细模式"
                : "[UI] 精简模式";
        printStyled(
                TerminalLayout.truncate(line, terminalWidth()),
                AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN),
                currentMode());
        writer.flush();
    }

    @Override
    public String readLine(String prompt) {
        if (closed.get()) {
            return null;
        }
        TerminalMode mode = currentMode();
        UiVerbosity currentVerbosity = verbosity.get();
        String effectivePrompt = context == null
                ? prompt
                : layout.primaryPrompt(mode, currentVerbosity);
        if (context != null) {
            String top = layout.inputTop(terminalWidth(), mode, currentVerbosity);
            if (!top.isEmpty()) {
                printStyled(top, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN), mode);
            }
            lineReader.setVariable(
                    LineReader.SECONDARY_PROMPT_PATTERN,
                    layout.continuationPrompt(mode, currentVerbosity));
            writer.flush();
        }
        try {
            return lineReader.readLine(styledPrompt(effectivePrompt, mode));
        } catch (UserInterruptException exception) {
            interruptHandler.get().run();
            return null;
        } catch (EndOfFileException exception) {
            return null;
        } finally {
            if (context != null && !closed.get() && displayPolicy().showStateTransitions()) {
                String status = layout.statusLine(
                        context, state.get(), terminalWidth(), mode, verbosity.get());
                if (!status.isEmpty()) {
                    printStyled(status, stateStyle(state.get()), mode);
                }
                writer.flush();
            }
        }
    }

    @Override
    public synchronized void beginAssistantResponse() {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        TerminalMode mode = currentMode();
        String prefix;
        if (verbosity.get() == UiVerbosity.COMPACT) {
            prefix = mode == TerminalMode.PLAIN ? "> " : "› ";
        } else {
            prefix = mode == TerminalMode.PLAIN ? "ImioCode> " : "ImioCode › ";
        }
        writer.print(styledPrompt(prefix, mode));
        writer.flush();
        assistantLineOpen = true;
    }

    @Override
    public synchronized void appendAssistantText(String text) {
        if (closed.get() || text == null || text.isEmpty()) {
            return;
        }
        writer.print(text);
        writer.flush();
    }

    @Override
    public synchronized void endAssistantResponse() {
        finishOpenAssistantLine();
    }

    @Override
    public synchronized void beginThinking() {
        if (closed.get() || thinkingLineOpen || !displayPolicy().showThinking()) {
            return;
        }
        finishOpenAssistantLine();
        TerminalMode mode = currentMode();
        String prefix = mode == TerminalMode.PLAIN ? "[thinking] " : "thinking › ";
        if (mode == TerminalMode.PLAIN) {
            writer.print(prefix);
        } else {
            writer.print(new AttributedString(
                    prefix,
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN).boldOff()).toAnsi(terminal));
        }
        writer.flush();
        thinkingLineOpen = true;
    }

    @Override
    public synchronized void appendThinkingText(String text) {
        if (closed.get() || text == null || text.isEmpty() || !displayPolicy().showThinking()) {
            return;
        }
        if (!thinkingLineOpen) {
            beginThinking();
        }
        if (currentMode() == TerminalMode.PLAIN) {
            writer.print(text);
        } else {
            writer.print(new AttributedString(
                    text,
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)).toAnsi(terminal));
        }
        writer.flush();
    }

    @Override
    public synchronized void endThinking() {
        if (!displayPolicy().showThinking()) {
            return;
        }
        finishOpenThinkingLine();
    }

    @Override
    public synchronized void showUsage(io.imiocode.llm.TokenUsage usage) {
        String formatted = usageFormatter.format(usage);
        if (closed.get() || formatted.isEmpty() || !displayPolicy().showUsage()) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        printStyled(formatted, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN), currentMode());
        writer.flush();
    }

    @Override
    public synchronized void showToolEvent(ToolExecutionEvent event) {
        if (closed.get() || !displayPolicy().showToolEvent(event.state())) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        TerminalMode mode = currentMode();
        if (verbosity.get() == UiVerbosity.COMPACT) {
            boolean succeeded = event.state() == ToolExecutionState.SUCCEEDED;
            String marker = mode == TerminalMode.PLAIN
                    ? (succeeded ? "[ok]" : "[fail]")
                    : (succeeded ? "✓" : "✗");
            String line = marker + " " + toolFormatter.compactSummary(event);
            AttributedStyle style = AttributedStyle.DEFAULT.foreground(
                    succeeded ? AttributedStyle.GREEN : AttributedStyle.RED);
            printStyled(TerminalLayout.truncate(line, terminalWidth()), style, mode);
            writer.flush();
            return;
        }
        String marker = switch (event.state()) {
            case QUEUED -> "○";
            case RUNNING -> "▶";
            case SUCCEEDED -> "✓";
            case FAILED -> "✗";
        };
        String line = marker + " [" + event.call().name() + " · "
                + toolFormatter.riskLabel(event.call()) + " · "
                + event.state().name().toLowerCase(java.util.Locale.ROOT) + "] "
                + toolFormatter.inputSummary(event.call()) + " — "
                + toolFormatter.resultSummary(event);
        AttributedStyle style = switch (event.state()) {
            case QUEUED -> AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
            case RUNNING -> AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
            case SUCCEEDED -> AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
            case FAILED -> AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
        };
        printStyled(TerminalLayout.truncate(line, terminalWidth()), style, mode);
        writer.flush();
    }

    @Override
    public synchronized PermissionReply confirmPermission(PermissionPrompt prompt) {
        Objects.requireNonNull(prompt, "prompt 不能为空");
        if (closed.get()) {
            return PermissionReply.DENY;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        TerminalMode mode = currentMode();
        printStyled(
                "[权限确认] 工具=" + prompt.toolName()
                        + "  风险=" + prompt.risk().name().toLowerCase(java.util.Locale.ROOT),
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW),
                mode);
        writer.println("目标: " + prompt.targetSummary());
        writer.println("原因: " + prompt.reason());
        writer.println("[1] 允许一次  [2] 本次会话允许相同操作  [3] 拒绝");
        writer.flush();
        while (!closed.get()) {
            try {
                String answer = lineReader.readLine(styledPrompt("选择> ", mode));
                if (answer == null) {
                    return PermissionReply.DENY;
                }
                switch (answer.trim().toLowerCase(java.util.Locale.ROOT)) {
                    case "1", "once" -> {
                        return PermissionReply.ALLOW_ONCE;
                    }
                    case "2", "session" -> {
                        return PermissionReply.ALLOW_SESSION;
                    }
                    case "3", "deny", "no", "n" -> {
                        return PermissionReply.DENY;
                    }
                    default -> {
                        writer.println("请输入 1、2 或 3。");
                        writer.flush();
                    }
                }
            } catch (UserInterruptException exception) {
                interruptHandler.get().run();
                return PermissionReply.DENY;
            } catch (EndOfFileException exception) {
                return PermissionReply.DENY;
            }
        }
        return PermissionReply.DENY;
    }

    @Override
    public synchronized boolean confirmAction(ConfirmationPrompt prompt) {
        Objects.requireNonNull(prompt, "prompt 不能为空");
        if (closed.get()) return false;
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        TerminalMode mode = currentMode();
        printStyled("[确认] " + prompt.title(),
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW), mode);
        writer.println("目标: " + prompt.detail());
        if (!prompt.risk().isBlank()) writer.println("风险: " + prompt.risk());
        writer.println("确认执行？[y/N]");
        writer.flush();
        try {
            String answer = lineReader.readLine(styledPrompt("选择> ", mode));
            return answer != null && (answer.trim().equalsIgnoreCase("y")
                    || answer.trim().equalsIgnoreCase("yes"));
        } catch (UserInterruptException | EndOfFileException exception) {
            return false;
        }
    }

    @Override
    public synchronized boolean approve(McpLaunchRequest request) {
        Objects.requireNonNull(request, "request 不能为空");
        if (closed.get()) {
            return false;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        TerminalMode mode = currentMode();
        printStyled(
                "[MCP 启动确认] Server=" + request.serverName(),
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW),
                mode);
        writer.println("命令: " + request.command());
        writer.println("参数: " + String.join(" ", request.args()));
        writer.println("[1] 允许启动  [2] 拒绝");
        writer.flush();
        while (!closed.get()) {
            try {
                String answer = lineReader.readLine(styledPrompt("选择> ", mode));
                if (answer == null) {
                    return false;
                }
                switch (answer.trim().toLowerCase(java.util.Locale.ROOT)) {
                    case "1", "yes", "y", "allow" -> {
                        return true;
                    }
                    case "2", "no", "n", "deny" -> {
                        return false;
                    }
                    default -> {
                        writer.println("请输入 1 或 2。");
                        writer.flush();
                    }
                }
            } catch (UserInterruptException exception) {
                interruptHandler.get().run();
                return false;
            } catch (EndOfFileException exception) {
                return false;
            }
        }
        return false;
    }

    @Override
    public synchronized void onMcpEvent(McpEvent event) {
        if (closed.get() || !displayPolicy().showMcpEvent(event.type())) {
            return;
        }
        String prefix = "[MCP/" + event.serverName() + "] ";
        String text = switch (event.type()) {
            case WAITING_FOR_APPROVAL -> "等待启动确认";
            case APPROVED -> "已批准启动";
            case DENIED -> "已拒绝启动";
            case CONNECTING -> "正在连接";
            case CONNECTED -> event.safeMessage();
            case TOOL_DISCOVERED -> "已注册工具 " + event.toolName();
            case SERVER_FAILED -> "连接失败: " + event.safeMessage();
            case CLOSED -> "连接已关闭";
        };
        if (event.type() == io.imiocode.mcp.manager.McpEventType.SERVER_FAILED) {
            printError(prefix + text);
        } else {
            printInfo(prefix + text);
        }
    }

    @Override
    public synchronized void showPermissionResolved(PermissionReply reply) {
        if (closed.get()) {
            return;
        }
        String message = switch (reply) {
            case ALLOW_ONCE -> "[权限] 已允许本次操作";
            case ALLOW_SESSION -> "[权限] 本次会话已允许相同操作";
            case DENY -> "[权限] 已拒绝操作";
        };
        int color = reply == PermissionReply.DENY
                ? AttributedStyle.RED
                : AttributedStyle.GREEN;
        printStyled(message, AttributedStyle.DEFAULT.foreground(color), currentMode());
        writer.flush();
    }

    @Override
    public synchronized void showAgentMode(AgentMode mode) {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        String text = mode == AgentMode.PLAN
                ? "[模式] Plan：仅可读取和搜索，最终输出实施计划"
                : "[模式] Do：允许使用当前全部已启用工具";
        printStyled(text, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN), currentMode());
        writer.flush();
    }

    @Override
    public synchronized void showAgentStop(
            AgentStopReason reason,
            boolean sideEffectsPossible
    ) {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        String reasonText = switch (reason) {
            case MAX_ITERATIONS -> "已达到最大循环轮数";
            case TIMEOUT -> "任务执行超时";
            case CANCELLED -> "任务已取消";
            case TOO_MANY_UNKNOWN_TOOLS -> "模型连续请求不存在的工具，任务已停止";
            case ERROR -> "执行失败";
            case FINAL_RESPONSE -> "任务已完成";
        };
        String suffix = sideEffectsPossible ? "；部分操作可能已经执行" : "";
        printStyled(
                "[Agent] " + reasonText + suffix,
                AttributedStyle.DEFAULT.foreground(AttributedStyle.RED),
                currentMode()
        );
        writer.flush();
    }

    @Override
    public synchronized void showRetry(AgentEvent.RetryScheduled retry) {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        String delay = retry.delay().isZero()
                ? "立即"
                : retry.delay().toMillis() + " ms 后";
        String line = "[重试] " + delay + "开始第 " + retry.nextAttempt()
                + " 次尝试；原因=" + retry.reason().name().toLowerCase(java.util.Locale.ROOT)
                + "；输出上限=" + retry.outputTokenLimit();
        printStyled(line, AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW), currentMode());
        writer.flush();
    }

    @Override
    public synchronized void showContextEvent(ContextEvent event) {
        if (closed.get() || !displayPolicy().showContextEvent(event)) return;
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        String line;
        int color = AttributedStyle.CYAN;
        if (event instanceof ContextEvent.ResultsOffloaded offloaded) {
            line = "[上下文] 已落盘 " + offloaded.count() + " 个大结果到 .imiocode/tool-results/";
        } else if (event instanceof ContextEvent.Started started) {
            line = "[上下文] 正在压缩，约 " + started.beforeTokens() + " Token";
        } else if (event instanceof ContextEvent.Completed completed) {
            line = "[上下文] 压缩完成：" + completed.beforeTokens() + " → " + completed.afterTokens() + " Token";
            color = AttributedStyle.GREEN;
        } else if (event instanceof ContextEvent.Failed failed) {
            line = "[上下文] " + failed.safeMessage();
            color = AttributedStyle.RED;
        } else {
            line = "[上下文] 自动摘要连续失败，当前任务已暂停自动摘要";
            color = AttributedStyle.YELLOW;
        }
        printStyled(line, AttributedStyle.DEFAULT.foreground(color), currentMode());
        writer.flush();
    }

    @Override
    public synchronized void showCompactReport(CompactReport report) {
        if (closed.get()) return;
        String line = report.compacted()
                ? "[上下文] 手动压缩完成：" + report.beforeTokens() + " → " + report.afterTokens()
                    + " Token，节省 " + Math.round(report.savedRatio() * 100) + "%"
                : "[上下文] " + report.message();
        printStyled(line, AttributedStyle.DEFAULT.foreground(
                report.compacted() ? AttributedStyle.GREEN : AttributedStyle.CYAN), currentMode());
        writer.flush();
    }

    @Override
    public synchronized void printError(String message) {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        writer.println("[错误] " + message);
        writer.flush();
    }

    @Override
    public synchronized void printInfo(String message) {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        writer.println(message);
        writer.flush();
    }

    @Override
    public void setInterruptHandler(Runnable handler) {
        interruptHandler.set(Objects.requireNonNull(handler, "handler"));
    }

    private void installMultilineWidget() {
        lineReader.getWidgets().put("insert-newline", () -> {
            lineReader.getBuffer().write('\n');
            return true;
        });
        Reference insertNewline = new Reference("insert-newline");
        lineReader.getKeyMaps().values().forEach(keyMap -> keyMap.bind(
                insertNewline,
                KeyMap.alt('\r'),
                KeyMap.alt('\n')));
    }

    private TerminalMode currentMode() {
        return TerminalMode.select(terminalWidth(), supportsAnsi());
    }

    private UiDisplayPolicy displayPolicy() {
        return new UiDisplayPolicy(verbosity.get());
    }

    private int terminalWidth() {
        int width = terminal.getWidth();
        return width > 0 ? width : 80;
    }

    private boolean supportsAnsi() {
        String type = terminal.getType();
        return type != null && !type.toLowerCase(java.util.Locale.ROOT).startsWith("dumb");
    }

    private String styledPrompt(String text, TerminalMode mode) {
        if (mode == TerminalMode.PLAIN) {
            return text;
        }
        return new AttributedString(text, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                .toAnsi(terminal);
    }

    private void printStyled(String text, AttributedStyle style, TerminalMode mode) {
        if (mode == TerminalMode.PLAIN) {
            writer.println(text);
        } else {
            new AttributedString(text, style).println(terminal);
        }
    }

    private static AttributedStyle stateStyle(UiState state) {
        int color = switch (state) {
            case READY -> AttributedStyle.GREEN;
            case THINKING -> AttributedStyle.YELLOW;
            case STREAMING -> AttributedStyle.CYAN;
            case TOOL_WAITING -> AttributedStyle.YELLOW;
            case TOOL_RUNNING -> AttributedStyle.CYAN;
            case PERMISSION_WAITING -> AttributedStyle.YELLOW;
            case COMPACTING -> AttributedStyle.YELLOW;
            case ERROR -> AttributedStyle.RED;
        };
        return AttributedStyle.DEFAULT.foreground(color);
    }

    private void finishOpenAssistantLine() {
        if (assistantLineOpen) {
            writer.println();
            writer.flush();
            assistantLineOpen = false;
        }
    }

    private void finishOpenThinkingLine() {
        if (thinkingLineOpen) {
            writer.println();
            writer.flush();
            thinkingLineOpen = false;
        }
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        finishOpenAssistantLine();
        finishOpenThinkingLine();
        try {
            terminal.close();
        } catch (IOException ignored) {
            // 终端关闭失败不应覆盖程序的原始退出原因。
        }
    }
}
