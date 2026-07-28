package io.imiocode.agent;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

final class DefaultRetryWaiter implements RetryWaiter {
    private static final long MAX_SLICE_NANOS = Duration.ofMillis(50).toNanos();

    @Override
    public void await(Duration delay, AgentTaskContext task) throws InterruptedException {
        Objects.requireNonNull(delay, "delay 不能为空");
        Objects.requireNonNull(task, "task 不能为空");
        long remaining = delay.toNanos();
        while (remaining > 0 && task.stopReason().isEmpty()) {
            if (Thread.interrupted()) {
                throw new InterruptedException("重试等待已中断");
            }
            long slice = Math.min(remaining, Math.min(MAX_SLICE_NANOS,
                    Math.max(0, task.remainingTime().toNanos())));
            if (slice <= 0) {
                return;
            }
            LockSupport.parkNanos(slice);
            remaining -= slice;
        }
    }
}
