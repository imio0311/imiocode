package io.imiocode.conversation;

import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiState;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.llm.LlmEvent;

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
            try {
                session.sendWithEvents(input, new ConversationListener() {
                    private boolean responseLineStarted;
                    private boolean thinkingLineStarted;
                    private int responseCount;

                    @Override
                    public void onResponseStarted() {
                        if (responseCount++ > 0) {
                            terminal.updateState(UiState.THINKING);
                        }
                        responseLineStarted = false;
                        thinkingLineStarted = false;
                    }

                    @Override
                    public void onTextDelta(String text) {
                        if (thinkingLineStarted) {
                            terminal.endThinking();
                            thinkingLineStarted = false;
                        }
                        if (!responseLineStarted) {
                            responseLineStarted = true;
                            terminal.updateState(UiState.STREAMING);
                            terminal.beginAssistantResponse();
                        }
                        terminal.appendAssistantText(text);
                    }

                    @Override
                    public void onLlmEvent(LlmEvent event) {
                        if (event instanceof LlmEvent.TextDelta delta) {
                            onTextDelta(delta.text());
                        } else if (event instanceof LlmEvent.ThinkingDelta delta) {
                            if (!thinkingLineStarted) {
                                thinkingLineStarted = true;
                                terminal.beginThinking();
                            }
                            terminal.appendThinkingText(delta.text());
                        } else if (event instanceof LlmEvent.ThinkingCompleted) {
                            terminal.endThinking();
                            thinkingLineStarted = false;
                        } else if (event instanceof LlmEvent.ToolCallStarted) {
                            terminal.endThinking();
                            terminal.endAssistantResponse();
                            terminal.updateState(UiState.TOOL_WAITING);
                        } else if (event instanceof LlmEvent.StreamCompleted completed) {
                            terminal.endThinking();
                            terminal.endAssistantResponse();
                            terminal.showUsage(completed.usage());
                        }
                    }

                    @Override
                    public void onResponseCompleted() {
                        terminal.endThinking();
                        terminal.endAssistantResponse();
                    }

                    @Override
                    public void onToolEvent(ToolExecutionEvent event) {
                        terminal.endAssistantResponse();
                        if (event.state() == ToolExecutionState.QUEUED) {
                            terminal.updateState(UiState.TOOL_WAITING);
                        } else if (event.state() == ToolExecutionState.RUNNING) {
                            terminal.updateState(UiState.TOOL_RUNNING);
                        }
                        terminal.showToolEvent(event);
                    }
                });
                terminal.updateState(UiState.READY);
            } catch (ConversationException exception) {
                terminal.endAssistantResponse();
                if (stopping.get() || exception.interrupted()) {
                    break;
                }
                terminal.updateState(UiState.ERROR);
                String suffix = exception.toolsExecuted()
                        ? "；工具已经执行，但本轮历史未保存"
                        : "，本轮响应未完成";
                if (exception.retryAfter().isPresent()) {
                    long seconds = exception.retryAfter().orElseThrow().toSeconds();
                    suffix += "；建议 " + seconds + " 秒后重试";
                }
                terminal.printError(exception.safeMessage() + suffix);
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
