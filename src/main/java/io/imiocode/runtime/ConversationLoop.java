package io.imiocode.runtime;

import io.imiocode.agent.AgentEvent;
import io.imiocode.agent.AgentStopReason;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandOutcome;
import io.imiocode.command.CommandRegistry;
import io.imiocode.conversation.ConversationException;
import io.imiocode.conversation.ConversationListener;
import io.imiocode.permission.PermissionReply;
import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiState;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.tool.ToolExecutionState;
import io.imiocode.subagent.task.TaskManager;
import io.imiocode.subagent.task.TaskNotification;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** 终端交互循环：本地命令优先，普通输入才进入 Agent。 */
public final class ConversationLoop {
    private final ConversationCoordinator coordinator;
    private final TerminalUi terminal;
    private final CommandRegistry commands;
    private final TaskManager tasks;
    private final AtomicBoolean stopping = new AtomicBoolean();

    public ConversationLoop(ConversationCoordinator coordinator, TerminalUi terminal, CommandRegistry commands) {
        this(coordinator, terminal, commands, null);
    }

    public ConversationLoop(ConversationCoordinator coordinator, TerminalUi terminal,
                            CommandRegistry commands, TaskManager tasks) {
        this.coordinator = Objects.requireNonNull(coordinator);
        this.terminal = Objects.requireNonNull(terminal);
        this.commands = Objects.requireNonNull(commands);
        this.tasks = tasks;
    }

    public void run() {
        terminal.setInterruptHandler(this::handleInterrupt);
        CommandContext commandContext = new CommandContext(coordinator, terminal, commands);
        terminal.refreshStatus(coordinator.status());
        while (!stopping.get()) {
            drainHookNotifications();
            drainTaskNotifications();
            String input = terminal.readLine("You> ");
            if (input == null || stopping.get()) break;
            if (input.trim().isEmpty()) continue;
            var command = commands.dispatch(input, commandContext);
            if (command.isPresent()) {
                command.orElseThrow().messages().forEach(message -> {
                    if (message.error()) terminal.printError(message.text()); else terminal.printInfo(message.text());
                });
                CommandOutcome outcome = command.orElseThrow().outcome();
                if (outcome == CommandOutcome.EXIT_REQUESTED) {
                    requestStop(); break;
                }
                if (outcome == CommandOutcome.RESTART_REQUESTED) {
                    requestStop(); break;
                }
                if (outcome == CommandOutcome.FORWARD_TO_AGENT) {
                    runAgent(command.orElseThrow().prompt().orElseThrow());
                } else {
                    terminal.refreshStatus(coordinator.status());
                }
                drainHookNotifications();
                drainTaskNotifications();
                continue;
            }
            runAgent(input);
        }
    }

    private void runAgent(String input) {
        terminal.updateState(UiState.THINKING);
        try {
            coordinator.sendWithEvents(input, new UiConversationListener());
            terminal.updateState(UiState.READY);
        } catch (ConversationException exception) {
            terminal.endAssistantResponse();
            if (stopping.get() || exception.interrupted()) return;
            if (exception.stopReason() != AgentStopReason.ERROR) {
                terminal.updateState(UiState.READY); return;
            }
            terminal.updateState(UiState.ERROR);
            String suffix = exception.sideEffectsPossible() ? "；部分操作可能已经执行，本轮历史未保存"
                    : exception.toolsExecuted() ? "；工具已经执行，但本轮历史未保存" : "，本轮响应未完成";
            terminal.printError(exception.safeMessage() + suffix);
        } catch (RuntimeException exception) {
            terminal.endAssistantResponse(); terminal.updateState(UiState.ERROR);
            terminal.printError("对话处理发生未知错误，本轮响应未完成");
        } finally {
            drainHookNotifications();
            drainTaskNotifications();
            if (!stopping.get()) terminal.refreshStatus(coordinator.status());
        }
    }

    private void drainHookNotifications() {
        coordinator.drainHookNotifications().forEach(terminal::showHookNotification);
    }

    private void drainTaskNotifications() {
        if (tasks == null) return;
        for (TaskNotification notification : tasks.drainNotifications()) {
            String text = "[任务/" + notification.taskId() + "] "
                    + notification.status().name().toLowerCase() + " · " + notification.summary();
            terminal.printInfo(text);
            coordinator.addSystemReminder("后台子 Agent 任务已结束：" + text
                    + "。结合此结果继续回答用户；不要重复声明这条通知。");
        }
    }

    public void requestStop() {
        if (stopping.compareAndSet(false, true)) {
            coordinator.cancelActive(); coordinator.close();
        }
    }

    private void handleInterrupt() {
        if (coordinator.isActive()) {
            coordinator.cancelActive();
            terminal.printInfo("[Agent] 已中断前台等待；正在运行的子 Agent 会转入任务表，可用 /tasks 查看");
            return;
        }
        requestStop();
    }

    private final class UiConversationListener implements ConversationListener {
        private boolean responseLineStarted;
        private boolean thinkingLineStarted;

        @Override public void onTextDelta(String text) {
            if (thinkingLineStarted) { terminal.endThinking(); thinkingLineStarted = false; }
            if (!responseLineStarted) {
                responseLineStarted = true; terminal.updateState(UiState.STREAMING); terminal.beginAssistantResponse();
            }
            terminal.appendAssistantText(text);
        }

        @Override public void onAgentEvent(AgentEvent event) {
            if (event instanceof AgentEvent.IterationStarted) { finishLines(); updateState(UiState.THINKING); }
            else if (event instanceof AgentEvent.RetryScheduled retry) { finishLines(); terminal.showRetry(retry); updateState(UiState.THINKING); }
            else if (event instanceof AgentEvent.TextDelta delta) onTextDelta(delta.text());
            else if (event instanceof AgentEvent.ThinkingDelta delta) {
                if (!thinkingLineStarted) { thinkingLineStarted = true; terminal.beginThinking(); }
                terminal.appendThinkingText(delta.text());
            } else if (event instanceof AgentEvent.ThinkingCompleted) { terminal.endThinking(); thinkingLineStarted = false; }
            else if (event instanceof AgentEvent.ModelToolRequested) { finishLines(); terminal.updateState(UiState.TOOL_WAITING); }
            else if (event instanceof AgentEvent.ModelResponseCompleted completed) { finishLines(); terminal.showUsage(completed.usage()); }
            else if (event instanceof AgentEvent.ToolExecutionChanged changed) onToolEvent(changed.execution());
            else if (event instanceof AgentEvent.PermissionRequested requested) {
                finishLines(); terminal.updateState(UiState.PERMISSION_WAITING);
                PermissionReply reply = terminal.confirmPermission(requested.prompt());
                if (!coordinator.respondPermission(requested.prompt().requestId(), reply) && !stopping.get()) {
                    terminal.printError("权限请求已失效");
                }
            } else if (event instanceof AgentEvent.PermissionResolved resolved) {
                terminal.showPermissionResolved(resolved.reply()); updateState(UiState.TOOL_WAITING);
            } else if (event instanceof AgentEvent.ContextChanged changed) terminal.showContextEvent(changed.event());
            else if (event instanceof AgentEvent.TaskCompleted || event instanceof AgentEvent.TaskFailed) finishLines();
            else if (event instanceof AgentEvent.TaskStopped stopped) {
                finishLines(); terminal.showAgentStop(stopped.reason(), stopped.sideEffectsPossible());
            }
        }

        @Override public void onToolEvent(ToolExecutionEvent event) {
            if (responseLineStarted) { terminal.endAssistantResponse(); responseLineStarted = false; }
            if (event.state() == ToolExecutionState.QUEUED) updateState(UiState.TOOL_WAITING);
            else if (event.state() == ToolExecutionState.RUNNING) updateState(UiState.TOOL_RUNNING);
            terminal.showToolEvent(event);
        }

        private void finishLines() {
            if (thinkingLineStarted) { terminal.endThinking(); thinkingLineStarted = false; }
            if (responseLineStarted) { terminal.endAssistantResponse(); responseLineStarted = false; }
        }
        private void updateState(UiState next) { if (terminal.state() != next) terminal.updateState(next); }
    }
}
