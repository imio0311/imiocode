package io.imiocode.agent;

import java.time.Duration;

/** 在可取消的 Agent 任务上下文中等待下一次重试。 */
@FunctionalInterface
interface RetryWaiter {
    void await(Duration delay, AgentTaskContext task) throws InterruptedException;
}
