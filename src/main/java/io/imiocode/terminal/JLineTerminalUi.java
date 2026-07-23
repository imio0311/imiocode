package io.imiocode.terminal;

import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
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
    private final AtomicReference<Runnable> interruptHandler = new AtomicReference<>(() -> { });
    private final AtomicReference<UiState> state = new AtomicReference<>(UiState.READY);
    private final AtomicBoolean closed = new AtomicBoolean();
    private UiContext context;
    private boolean assistantLineOpen;

    public JLineTerminalUi() throws IOException {
        this(TerminalBuilder.builder().system(true).encoding(java.nio.charset.StandardCharsets.UTF_8).build(),
                new SecretRedactor(""));
    }

    public JLineTerminalUi(SecretRedactor redactor) throws IOException {
        this(TerminalBuilder.builder().system(true).encoding(java.nio.charset.StandardCharsets.UTF_8).build(),
                redactor);
    }

    JLineTerminalUi(Terminal terminal) {
        this(terminal, LineReaderBuilder.builder().terminal(terminal).build(), new SecretRedactor(""));
    }

    JLineTerminalUi(Terminal terminal, SecretRedactor redactor) {
        this(terminal, LineReaderBuilder.builder().terminal(terminal).build(), redactor);
    }

    JLineTerminalUi(Terminal terminal, LineReader lineReader) {
        this(terminal, lineReader, new SecretRedactor(""));
    }

    JLineTerminalUi(Terminal terminal, LineReader lineReader, SecretRedactor redactor) {
        this.terminal = Objects.requireNonNull(terminal, "terminal");
        this.lineReader = Objects.requireNonNull(lineReader, "lineReader");
        this.writer = terminal.writer();
        this.toolFormatter = new ToolSummaryFormatter(redactor);
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
        for (String line : layout.welcome(context, state.get(), terminalWidth(), mode)) {
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
        finishOpenAssistantLine();
        TerminalMode mode = currentMode();
        printStyled(layout.statusLine(context, next, terminalWidth(), mode), stateStyle(next), mode);
        writer.flush();
    }

    @Override
    public UiState state() {
        return state.get();
    }

    @Override
    public String readLine(String prompt) {
        if (closed.get()) {
            return null;
        }
        TerminalMode mode = currentMode();
        String effectivePrompt = context == null ? prompt : layout.primaryPrompt(mode);
        if (context != null) {
            String top = layout.inputTop(terminalWidth(), mode);
            if (!top.isEmpty()) {
                printStyled(top, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN), mode);
            }
            lineReader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, layout.continuationPrompt(mode));
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
            if (context != null && !closed.get()) {
                printStyled(layout.statusLine(context, state.get(), terminalWidth(), mode),
                        stateStyle(state.get()), mode);
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
        TerminalMode mode = currentMode();
        String prefix = mode == TerminalMode.PLAIN ? "ImioCode> " : "ImioCode › ";
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
    public synchronized void showToolEvent(ToolExecutionEvent event) {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
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
        TerminalMode mode = currentMode();
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
    public synchronized void printError(String message) {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
        writer.println("[错误] " + message);
        writer.flush();
    }

    @Override
    public synchronized void printInfo(String message) {
        if (closed.get()) {
            return;
        }
        finishOpenAssistantLine();
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

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        finishOpenAssistantLine();
        try {
            terminal.close();
        } catch (IOException ignored) {
            // 终端关闭失败不应覆盖程序的原始退出原因。
        }
    }
}
