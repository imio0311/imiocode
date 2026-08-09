package io.imiocode.hook;

import io.imiocode.conversation.SystemReminder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/** 有界、线程安全的 Hook prompt 收件箱。容量满时淘汰最旧项。 */
public final class HookPromptInbox {
    private final int capacity;
    private final java.util.ArrayDeque<SystemReminder> queue = new java.util.ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    public HookPromptInbox(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity 必须大于 0");
        this.capacity = capacity;
    }

    public void offer(SystemReminder reminder) {
        lock.lock();
        try {
            while (queue.size() >= capacity) queue.removeFirst();
            queue.addLast(reminder);
        } finally {
            lock.unlock();
        }
    }

    public List<SystemReminder> drain() {
        lock.lock();
        try {
            List<SystemReminder> values = new ArrayList<>(queue);
            queue.clear();
            return List.copyOf(values);
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try { queue.clear(); } finally { lock.unlock(); }
    }
}
