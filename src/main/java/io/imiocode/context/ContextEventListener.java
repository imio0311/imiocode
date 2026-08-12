package io.imiocode.context;

/** 接收上下文压缩、外置和预算管理事件。 */
@FunctionalInterface
public interface ContextEventListener {
    ContextEventListener NOOP = event -> { };
    void onEvent(ContextEvent event);
}
