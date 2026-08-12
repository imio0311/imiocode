package io.imiocode.tool;

/** 接收一次工具调用从开始到完成的生命周期事件。 */
@FunctionalInterface
public interface ToolExecutionListener {
    ToolExecutionListener NOOP = event -> { };

    void onToolEvent(ToolExecutionEvent event);
}
