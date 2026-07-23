package io.imiocode.terminal;

import io.imiocode.tool.ToolExecutionEvent;

public interface TerminalUi extends AutoCloseable {
    void showWelcome(UiContext context);

    void updateState(UiState state);

    UiState state();

    String readLine(String prompt);

    void beginAssistantResponse();

    void appendAssistantText(String text);

    void endAssistantResponse();

    default void showToolEvent(ToolExecutionEvent event) {
    }

    void printError(String message);

    void printInfo(String message);

    void setInterruptHandler(Runnable handler);

    @Override
    void close();
}
