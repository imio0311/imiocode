package io.imiocode.context;

@FunctionalInterface
public interface ContextEventListener {
    ContextEventListener NOOP = event -> { };
    void onEvent(ContextEvent event);
}
