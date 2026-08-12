package io.imiocode.conversation;

import io.imiocode.agent.AgentEvent;
import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentStopReason;
import io.imiocode.config.UiVerbosity;
import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiState;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.permission.PermissionReply;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 驱动交互式终端会话，并把 Agent 生命周期事件映射为可见 UI 状态。
 *
 * <p>本循环只处理进程级兼容命令和渲染；其余 Slash Command 由会话协调器统一分流。</p>
 */
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
            if ("/plan".equalsIgnoreCase(trimmed)) {
                switchMode(AgentMode.PLAN);
                continue;
            }
            if ("/do".equalsIgnoreCase(trimmed)) {
                switchMode(AgentMode.DO);
                continue;
            }
            if ("/compact".equalsIgnoreCase(trimmed)) {
                compact();
                continue;
            }
            if ("/verbose".equalsIgnoreCase(trimmed)) {
                switchVerbosity(UiVerbosity.VERBOSE);
                continue;
            }
            if ("/compact-ui".equalsIgnoreCase(trimmed)) {
                switchVerbosity(UiVerbosity.COMPACT);
                continue;
            }

            terminal.updateState(UiState.THINKING);
            try {
                // 单个监听器维护思考行和回答行的开闭状态，避免流式事件交错时破坏终端布局。
                session.sendWithEvents(input, new ConversationListener() {
                    private boolean responseLineStarted;
                    private boolean thinkingLineStarted;

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
                    public void onAgentEvent(AgentEvent event) {
                        if (event instanceof AgentEvent.TaskStarted) {
                        } else if (event instanceof AgentEvent.IterationStarted) {
                            finishLines();
                            updateStateIfChanged(UiState.THINKING);
                        } else if (event instanceof AgentEvent.RetryScheduled retry) {
                            finishLines();
                            terminal.showRetry(retry);
                            updateStateIfChanged(UiState.THINKING);
                        } else if (event instanceof AgentEvent.TextDelta delta) {
                            onTextDelta(delta.text());
                        } else if (event instanceof AgentEvent.ThinkingDelta delta) {
                            if (!thinkingLineStarted) {
                                thinkingLineStarted = true;
                                terminal.beginThinking();
                            }
                            terminal.appendThinkingText(delta.text());
                        } else if (event instanceof AgentEvent.ThinkingCompleted) {
                            terminal.endThinking();
                            thinkingLineStarted = false;
                        } else if (event instanceof AgentEvent.ModelToolRequested) {
                            finishLines();
                            terminal.updateState(UiState.TOOL_WAITING);
                        } else if (event instanceof AgentEvent.ModelResponseCompleted completed) {
                            finishLines();
                            terminal.showUsage(completed.usage());
                        } else if (event instanceof AgentEvent.ToolBatchStarted) {
                        } else if (event instanceof AgentEvent.ToolExecutionChanged changed) {
                            onToolEvent(changed.execution());
                        } else if (event instanceof AgentEvent.PermissionRequested requested) {
                            finishLines();
                            terminal.updateState(UiState.PERMISSION_WAITING);
                            PermissionReply reply =
                                    terminal.confirmPermission(requested.prompt());
                            if (!session.respondPermission(
                                    requested.prompt().requestId(), reply)
                                    && !stopping.get()) {
                                terminal.printError("权限请求已失效");
                            }
                        } else if (event instanceof AgentEvent.PermissionResolved resolved) {
                            terminal.showPermissionResolved(resolved.reply());
                            updateStateIfChanged(UiState.TOOL_WAITING);
                        } else if (event instanceof AgentEvent.ModeChanged changed) {
                            terminal.showAgentMode(changed.current());
                        } else if (event instanceof AgentEvent.ContextChanged changed) {
                            terminal.showContextEvent(changed.event());
                        } else if (event instanceof AgentEvent.TaskCompleted) {
                            finishLines();
                        } else if (event instanceof AgentEvent.TaskStopped stopped) {
                            finishLines();
                            terminal.showAgentStop(
                                    stopped.reason(),
                                    stopped.sideEffectsPossible());
                        } else if (event instanceof AgentEvent.TaskFailed) {
                            finishLines();
                        }
                    }

                    @Override
                    public void onToolEvent(ToolExecutionEvent event) {
                        if (responseLineStarted) {
                            terminal.endAssistantResponse();
                            responseLineStarted = false;
                        }
                        if (event.state() == ToolExecutionState.QUEUED) {
                            updateStateIfChanged(UiState.TOOL_WAITING);
                        } else if (event.state() == ToolExecutionState.RUNNING) {
                            updateStateIfChanged(UiState.TOOL_RUNNING);
                        }
                        terminal.showToolEvent(event);
                    }

                    private void finishLines() {
                        if (thinkingLineStarted) {
                            terminal.endThinking();
                            thinkingLineStarted = false;
                        }
                        if (responseLineStarted) {
                            terminal.endAssistantResponse();
                            responseLineStarted = false;
                        }
                    }
                });
                terminal.updateState(UiState.READY);
            } catch (ConversationException exception) {
                terminal.endAssistantResponse();
                if (stopping.get() || exception.interrupted()) {
                    break;
                }
                if (exception.stopReason() != AgentStopReason.ERROR) {
                    terminal.updateState(UiState.READY);
                    continue;
                }
                terminal.updateState(UiState.ERROR);
                String suffix;
                if (exception.sideEffectsPossible()) {
                    suffix = "；部分操作可能已经执行，本轮历史未保存";
                } else if (exception.toolsExecuted()) {
                    suffix = "；工具已经执行，但本轮历史未保存";
                } else {
                    suffix = "，本轮响应未完成";
                }
                if (exception.retryAfter().isPresent()) {
                    long seconds = exception.retryAfter().orElseThrow().toSeconds();
                    suffix += "；建议 " + seconds + " 秒后重试";
                }
                terminal.printError(exception.safeMessage() + suffix);
            } catch (RuntimeException exception) {
                terminal.endAssistantResponse();
                terminal.updateState(UiState.ERROR);
                terminal.printError("对话处理发生未知错误，本轮响应未完成");
            }
        }
    }

    public void requestStop() {
        // 终端中断和 /exit 可能并发到达，只允许第一个请求关闭底层会话。
        if (stopping.compareAndSet(false, true)) {
            session.cancelActive();
            session.close();
        }
    }

    private void switchMode(AgentMode mode) {
        session.switchMode(mode, new ConversationListener() {
            @Override
            public void onTextDelta(String text) {
            }

            @Override
            public void onAgentEvent(AgentEvent event) {
                if (event instanceof AgentEvent.ModeChanged changed) {
                    terminal.showAgentMode(changed.current());
                }
            }
        });
    }

    private void compact() {
        terminal.updateState(UiState.COMPACTING);
        try {
            var report = session.forceCompact(new ConversationListener() {
                @Override public void onTextDelta(String text) { }
                @Override public void onAgentEvent(AgentEvent event) {
                    if (event instanceof AgentEvent.ContextChanged changed) {
                        terminal.showContextEvent(changed.event());
                    }
                }
            });
            terminal.showCompactReport(report);
        } catch (RuntimeException exception) {
            terminal.printError("上下文压缩失败，原历史已保留");
        } finally {
            if (!stopping.get()) terminal.updateState(UiState.READY);
        }
    }

    private void switchVerbosity(UiVerbosity verbosity) {
        terminal.setVerbosity(verbosity);
        terminal.showVerbosityChanged(verbosity);
    }

    private void updateStateIfChanged(UiState next) {
        if (terminal.state() != next) {
            terminal.updateState(next);
        }
    }

    private static boolean isExitCommand(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        return "/exit".equals(normalized) || "/quit".equals(normalized);
    }
}
