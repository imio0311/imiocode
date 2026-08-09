package io.imiocode.hook;

import io.imiocode.conversation.SystemReminder;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class HookQueueTest {
    @Test void promptInboxDropsOldestAndDrainClears() {
        HookPromptInbox inbox = new HookPromptInbox(2);
        inbox.offer(new SystemReminder("one"));
        inbox.offer(new SystemReminder("two"));
        inbox.offer(new SystemReminder("three"));
        assertEquals(java.util.List.of("two", "three"),
                inbox.drain().stream().map(SystemReminder::content).toList());
        assertTrue(inbox.drain().isEmpty());
    }

    @Test void notificationOverflowIsReportedOnce() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        HookNotificationQueue queue = new HookNotificationQueue(1, clock);
        queue.offer(notification("one"));
        queue.offer(notification("two"));
        var drained = queue.drain();
        assertEquals(2, drained.size());
        assertTrue(drained.getFirst().summary().contains("丢弃 1"));
        assertEquals("two", drained.getLast().hookId());
        assertTrue(queue.drain().isEmpty());
    }

    private static HookNotification notification(String id) {
        return new HookNotification(Instant.EPOCH, id, HookEvent.STARTUP,
                HookExecutionStatus.SUCCEEDED, "ok");
    }
}
