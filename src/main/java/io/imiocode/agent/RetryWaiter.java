package io.imiocode.agent;

import java.time.Duration;

@FunctionalInterface
interface RetryWaiter {
    void await(Duration delay, AgentTaskContext task) throws InterruptedException;
}
