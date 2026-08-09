package io.imiocode.terminal;

import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentEvent;
import io.imiocode.agent.AgentStopReason;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.llm.TokenUsage;
import io.imiocode.permission.PermissionPrompt;
import io.imiocode.permission.PermissionReply;
import io.imiocode.mcp.manager.McpEvent;
import io.imiocode.mcp.manager.McpEventListener;
import io.imiocode.mcp.manager.McpLaunchApprover;
import io.imiocode.mcp.manager.McpLaunchRequest;
import io.imiocode.context.CompactReport;
import io.imiocode.context.ContextEvent;
import io.imiocode.config.UiVerbosity;
import io.imiocode.command.CommandStatus;
import io.imiocode.command.ConfirmationPrompt;
import io.imiocode.command.UIController;
import io.imiocode.hook.HookExecutionStatus;
import io.imiocode.hook.HookNotification;

public interface TerminalUi extends AutoCloseable, McpLaunchApprover, McpEventListener, UIController {
    void showWelcome(UiContext context);

    void updateState(UiState state);

    UiState state();

    default UiVerbosity verbosity() {
        return UiVerbosity.COMPACT;
    }

    default void setVerbosity(UiVerbosity verbosity) {
    }

    default void showVerbosityChanged(UiVerbosity verbosity) {
        printInfo(verbosity == UiVerbosity.VERBOSE
                ? "[UI] 已切换为详细模式"
                : "[UI] 已切换为精简模式");
    }

    String readLine(String prompt);

    void beginAssistantResponse();

    void appendAssistantText(String text);

    void endAssistantResponse();

    default void beginThinking() {
    }

    default void appendThinkingText(String text) {
    }

    default void endThinking() {
    }

    default void showUsage(TokenUsage usage) {
    }

    default void showToolEvent(ToolExecutionEvent event) {
    }

    default PermissionReply confirmPermission(PermissionPrompt prompt) {
        return PermissionReply.DENY;
    }

    /** 通用破坏性操作确认，默认拒绝。 */
    @Override
    default boolean confirm(ConfirmationPrompt prompt) {
        return false;
    }

    @Override
    default void clearScreen() {
    }

    @Override
    default void refreshStatus(CommandStatus status) {
    }

    @Override
    default boolean approve(McpLaunchRequest request) {
        return false;
    }

    @Override
    default void onMcpEvent(McpEvent event) {
    }

    default void showPermissionResolved(PermissionReply reply) {
        printInfo("[权限] " + switch (reply) {
            case ALLOW_ONCE -> "已允许本次操作";
            case ALLOW_SESSION -> "本次会话已允许相同操作";
            case DENY -> "已拒绝操作";
        });
    }

    default void showAgentMode(AgentMode mode) {
        printInfo(mode == AgentMode.PLAN
                ? "[模式] Plan：仅启用只读工具"
                : "[模式] Do：已启用正常工具");
    }

    default void showAgentStop(AgentStopReason reason, boolean sideEffectsPossible) {
        String suffix = sideEffectsPossible ? "；部分操作可能已经执行" : "";
        printError("Agent 已停止：" + reason.name().toLowerCase(java.util.Locale.ROOT) + suffix);
    }

    default void showRetry(AgentEvent.RetryScheduled retry) {
        printInfo("[重试] 第 " + retry.nextAttempt() + " 次尝试，原因 "
                + retry.reason().name().toLowerCase(java.util.Locale.ROOT)
                + "，等待 " + retry.delay().toMillis() + " ms");
    }

    default void showContextEvent(ContextEvent event) {
        if (event instanceof ContextEvent.ResultsOffloaded offloaded) {
            printInfo("[上下文] 已将 " + offloaded.count() + " 个大工具结果写入 .imiocode/tool-results/");
        } else if (event instanceof ContextEvent.Started started) {
            printInfo("[上下文] 正在压缩，约 " + started.beforeTokens() + " Token");
        } else if (event instanceof ContextEvent.Completed completed) {
            printInfo("[上下文] 压缩完成：" + completed.beforeTokens() + " → " + completed.afterTokens() + " Token");
        } else if (event instanceof ContextEvent.Failed failed) {
            printError("[上下文] " + failed.safeMessage());
        } else if (event instanceof ContextEvent.CircuitOpened) {
            printError("[上下文] 自动摘要连续失败，当前任务已暂停自动摘要");
        }
    }

    default void showCompactReport(CompactReport report) {
        if (report.compacted()) {
            printInfo("[上下文] 手动压缩完成：" + report.beforeTokens() + " → " + report.afterTokens()
                    + " Token，节省 " + Math.round(report.savedRatio() * 100) + "%");
        } else {
            printInfo("[上下文] " + report.message());
        }
    }

    default void showHookNotification(HookNotification notification) {
        boolean important = notification.status() == HookExecutionStatus.FAILED
                || notification.status() == HookExecutionStatus.TIMED_OUT
                || notification.status() == HookExecutionStatus.REJECTED
                || notification.status() == HookExecutionStatus.NOT_IMPLEMENTED;
        if (!important && verbosity() != UiVerbosity.VERBOSE) return;
        String text = "[Hook/" + notification.hookId() + "] "
                + notification.event().configName() + " · "
                + notification.status().name().toLowerCase(java.util.Locale.ROOT)
                + (notification.summary().isBlank() ? "" : " · " + notification.summary());
        if (important) printError(text); else printInfo(text);
    }

    void printError(String message);

    void printInfo(String message);

    void setInterruptHandler(Runnable handler);

    @Override
    void close();
}
