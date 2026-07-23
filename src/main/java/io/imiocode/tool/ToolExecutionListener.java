package io.imiocode.tool;

@FunctionalInterface
public interface ToolExecutionListener {
    ToolExecutionListener NOOP = event -> { };

    void onToolEvent(ToolExecutionEvent event);
}
