package io.imiocode.conversation;

import io.imiocode.agent.AgentEvent;
import io.imiocode.tool.ToolExecutionEvent;
import io.imiocode.llm.LlmEvent;

/**
 * 将 Agent、LLM 和工具执行事件汇聚到终端会话 UI。
 *
 * <p>唯一抽象方法保持对纯文本消费者的函数式兼容；其他生命周期事件使用默认空实现，调用方可按需订阅。</p>
 */
@FunctionalInterface
public interface ConversationListener {
    void onTextDelta(String text);

    default void onAgentEvent(AgentEvent event) {
        // Agent 与旧版 LLM 流均统一回落到文本增量，避免 UI 维护两套文本渲染路径。
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
