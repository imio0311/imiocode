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

public interface TerminalUi extends AutoCloseable, McpLaunchApprover, McpEventListener {
    void showWelcome(UiContext context);

    void updateState(UiState state);

    UiState state();

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

    void printError(String message);

    void printInfo(String message);

    void setInterruptHandler(Runnable handler);

    @Override
    void close();
}
