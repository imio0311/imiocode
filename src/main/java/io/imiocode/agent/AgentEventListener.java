package io.imiocode.agent;

/**
 * Agent 事件监听器。实现方应避免阻塞事件生产线程。
 */
@FunctionalInterface
public interface AgentEventListener {
    AgentEventListener NOOP = event -> {
    };

    void onEvent(AgentEvent event);
}
