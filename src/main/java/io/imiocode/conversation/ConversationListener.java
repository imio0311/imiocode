package io.imiocode.conversation;

import io.imiocode.tool.ToolExecutionEvent;

@FunctionalInterface
public interface ConversationListener {
    void onTextDelta(String text);

    default void onResponseStarted() {
    }

    default void onResponseCompleted() {
    }

    default void onToolEvent(ToolExecutionEvent event) {
    }
}
