package io.imiocode.subagent.task;

import io.imiocode.agent.AgentStopReason;
import io.imiocode.subagent.definition.AgentDefinitionParser;
import io.imiocode.subagent.definition.AgentDefinitionSource;
import io.imiocode.subagent.runtime.SubagentRunResult;
import io.imiocode.subagent.trace.TraceTokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TaskManagerTest {
    @Test void completesNotifiesOnceAndCanBeInspected() throws Exception {
        CountDownLatch completed=new CountDownLatch(1);
        try (TaskManager tasks=new TaskManager((definition,task,history,background,mode,cancellation)->{
            completed.countDown();
            return new SubagentRunResult(true,"done", AgentStopReason.FINAL_RESPONSE, TraceTokenUsage.zero(),"trace");
        },1,8,8)) {
            String id=tasks.submit(definition(),"work",List.of());
            assertTrue(completed.await(2, TimeUnit.SECONDS));
            TaskSnapshot snapshot=tasks.adopt(id);
            assertEquals(TaskStatus.COMPLETED,snapshot.status());
            assertEquals(1,tasks.drainNotifications().size());
            assertTrue(tasks.drainNotifications().isEmpty());
        }
    }

    @Test void cancelInvokesIsolatedCancellationAndRecordsTerminalState() throws Exception {
        CountDownLatch started=new CountDownLatch(1); CountDownLatch release=new CountDownLatch(1);
        AtomicBoolean cancelled=new AtomicBoolean();
        try (TaskManager tasks=new TaskManager((definition,task,history,background,mode,cancellation)->{
            cancellation.register(()->{cancelled.set(true);release.countDown();}); started.countDown();
            try { release.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return new SubagentRunResult(false,"cancelled",AgentStopReason.CANCELLED,TraceTokenUsage.zero(),"trace");
        },1,8,8)) {
            String id=tasks.submit(definition(),"work",List.of());
            assertTrue(started.await(2,TimeUnit.SECONDS));
            assertTrue(tasks.cancel(id));
            assertTrue(cancelled.get());
            assertEquals(TaskStatus.CANCELLED,tasks.find(id).orElseThrow().status());
        }
    }

    @Test void detachedForegroundProducesOneBackgroundNotification() throws Exception {
        CountDownLatch started=new CountDownLatch(1); CountDownLatch release=new CountDownLatch(1);
        try (TaskManager tasks=new TaskManager((definition,task,history,background,mode,cancellation)->{
            started.countDown();
            try { release.await(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            return new SubagentRunResult(true,"detached done",AgentStopReason.FINAL_RESPONSE,TraceTokenUsage.zero(),"trace");
        },1,8,8)) {
            String id=tasks.submitForeground(definition(),"work",List.of());
            assertTrue(started.await(2,TimeUnit.SECONDS)); assertTrue(tasks.detach(id)); release.countDown();
            assertEquals(TaskStatus.COMPLETED,tasks.await(id).status());
            assertEquals(1,tasks.drainNotifications().size()); assertTrue(tasks.drainNotifications().isEmpty());
        }
    }

    private static io.imiocode.subagent.definition.AgentDefinition definition() {
        return new AgentDefinitionParser().parse("---\nname: worker\ndescription: worker\ntimeoutSeconds: 10\n---\nprompt",
                AgentDefinitionSource.PROJECT,null);
    }
}
