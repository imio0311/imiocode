package io.imiocode.llm;

@FunctionalInterface
public interface StreamListener extends LlmEventListener {
    void onTextDelta(String text);

    @Override
    default void onEvent(LlmEvent event) {
        if (event instanceof LlmEvent.TextDelta delta) {
            onTextDelta(delta.text());
        }
    }
}
