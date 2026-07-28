package io.imiocode.terminal;

import io.imiocode.agent.AgentMode;
import io.imiocode.agent.AgentEvent;
import io.imiocode.agent.AgentStopReason;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.llm.TokenUsage;

public interface TerminalUi extends AutoCloseable {
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

    void printError(String message);

    void printInfo(String message);

    void setInterruptHandler(Runnable handler);

    @Override
    void close();
}
