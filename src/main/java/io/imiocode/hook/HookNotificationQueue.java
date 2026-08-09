package io.imiocode.hook;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/** 有界通知队列；溢出时淘汰最旧项，并在下次 drain 生成一次汇总告警。 */
public final class HookNotificationQueue {
    private final int capacity;
    private final Clock clock;
    private final java.util.ArrayDeque<HookNotification> queue = new java.util.ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private long dropped;

    public HookNotificationQueue(int capacity, Clock clock) {
        if (capacity < 1) throw new IllegalArgumentException("capacity 必须大于 0");
        this.capacity = capacity;
        this.clock = clock;
    }

    public void offer(HookNotification notification) {
        lock.lock();
        try {
            if (queue.size() >= capacity) {
                queue.removeFirst();
                dropped++;
            }
            queue.addLast(notification);
        } finally {
            lock.unlock();
        }
    }

    public List<HookNotification> drain() {
        lock.lock();
        try {
            List<HookNotification> values = new ArrayList<>();
            if (dropped > 0) {
                values.add(new HookNotification(clock.instant(), "hook-notifications", HookEvent.ERROR,
                        HookExecutionStatus.FAILED, "Hook 通知队列已丢弃 " + dropped + " 条旧通知"));
                dropped = 0;
            }
            values.addAll(queue);
            queue.clear();
            return List.copyOf(values);
        } finally {
            lock.unlock();
        }
    }
}
