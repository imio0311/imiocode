package io.imiocode.conversation;

import io.imiocode.agent.AgentEvent;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.llm.LlmEvent;

@FunctionalInterface
public interface ConversationListener {
    void onTextDelta(String text);

    default void onAgentEvent(AgentEvent event) {
        if (event instanceof AgentEvent.TextDelta delta) {
            onTextDelta(delta.text());
        }
    }

    default void onLlmEvent(LlmEvent event) {
        if (event instanceof LlmEvent.TextDelta delta) {
            onTextDelta(delta.text());
        }
    }

    default void onResponseStarted() {
    }

    default void onResponseCompleted() {
    }

    default void onToolEvent(ToolExecutionEvent event) {
    }
}
