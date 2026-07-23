package io.imiocode.conversation;

import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;
import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiState;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConversationLoop {
    private final ConversationSession session;
    private final TerminalUi terminal;
    private final AtomicBoolean stopping = new AtomicBoolean();

    public ConversationLoop(ConversationSession session, TerminalUi terminal) {
        this.session = Objects.requireNonNull(session, "session");
        this.terminal = Objects.requireNonNull(terminal, "terminal");
    }

    public void run() {
        terminal.setInterruptHandler(this::requestStop);
        while (!stopping.get()) {
            String input = terminal.readLine("You> ");
            if (input == null || stopping.get()) {
                break;
            }
            String trimmed = input.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (isExitCommand(trimmed)) {
                requestStop();
                break;
            }

            terminal.updateState(UiState.THINKING);
            AtomicBoolean firstDelta = new AtomicBoolean(true);
            try {
                session.send(input, text -> {
                    if (firstDelta.compareAndSet(true, false)) {
                        terminal.updateState(UiState.STREAMING);
                        terminal.beginAssistantResponse();
                    }
                    terminal.appendAssistantText(text);
                });
                terminal.endAssistantResponse();
                terminal.updateState(UiState.READY);
            } catch (LlmException exception) {
                terminal.endAssistantResponse();
                if (stopping.get() || exception.type() == LlmErrorType.INTERRUPTED) {
                    break;
                }
                terminal.updateState(UiState.ERROR);
                terminal.printError(exception.safeMessage() + "，本轮响应未完成");
                if (!exception.recoverable()) {
                    requestStop();
                }
            } catch (RuntimeException exception) {
                terminal.endAssistantResponse();
                terminal.updateState(UiState.ERROR);
                terminal.printError("对话处理发生未知错误，本轮响应未完成");
            }
        }
    }

    public void requestStop() {
        if (stopping.compareAndSet(false, true)) {
            session.close();
        }
    }

    private static boolean isExitCommand(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        return "/exit".equals(normalized) || "/quit".equals(normalized);
    }
}
