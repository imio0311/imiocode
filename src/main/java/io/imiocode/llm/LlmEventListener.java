package io.imiocode.llm;

/** 接收 Provider 流式响应产生的结构化事件。 */
@FunctionalInterface
public interface LlmEventListener {
    void onEvent(LlmEvent event);
}
