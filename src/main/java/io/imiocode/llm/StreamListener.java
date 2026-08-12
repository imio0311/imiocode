package io.imiocode.llm;

/**
 * 只消费文本增量的兼容监听器。
 *
 * <p>推理、用量和生命周期事件会被有意忽略；需要这些信息的调用方应直接使用 {@link LlmEventListener}。</p>
 */
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
