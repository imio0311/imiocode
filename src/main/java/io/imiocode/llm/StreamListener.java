package io.imiocode.llm;

@FunctionalInterface
public interface StreamListener {
    void onTextDelta(String text);
}
