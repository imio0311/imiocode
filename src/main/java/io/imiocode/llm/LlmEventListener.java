package io.imiocode.llm;

@FunctionalInterface
public interface LlmEventListener {
    void onEvent(LlmEvent event);
}
