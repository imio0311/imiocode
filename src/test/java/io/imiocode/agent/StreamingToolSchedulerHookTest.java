package io.imiocode.agent;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.hook.*;
import io.imiocode.hook.integration.HookContextFactory;
import io.imiocode.tool.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StreamingToolSchedulerHookTest {
    @Test void rejectedPreToolRunsOnceAndSkipsToolAndPostHook() {
        ToolRegistry registry = new ToolRegistry();
        AtomicInteger toolRuns = new AtomicInteger();
        registry.register(tool("write_file", ToolRisk.LOW, toolRuns, true));
        RecordingHooks hooks = new RecordingHooks(true);
        try (StreamingToolScheduler scheduler = scheduler(registry, hooks)) {
            scheduler.onToolCallCompleted(0, call("write_file"));
            scheduler.onStreamCompleted();
            List<ToolExecution> results = scheduler.awaitResults();
            assertEquals(1, results.size());
            assertFalse(results.getFirst().result().success());
            assertTrue(results.getFirst().result().error().contains("blocked by hook test"));
        }
        assertEquals(1, hooks.pre.get());
        assertEquals(0, hooks.post.get());
        assertEquals(0, toolRuns.get());
    }

    @Test void realToolFailureStillEmitsPostToolExactlyOnce() {
        ToolRegistry registry = new ToolRegistry();
        AtomicInteger toolRuns = new AtomicInteger();
        registry.register(tool("grep", ToolRisk.LOW, toolRuns, false));
        RecordingHooks hooks = new RecordingHooks(false);
        try (StreamingToolScheduler scheduler = scheduler(registry, hooks)) {
            scheduler.onToolCallCompleted(0, call("grep"));
            scheduler.onStreamCompleted();
            assertFalse(scheduler.awaitResults().getFirst().result().success());
        }
        assertEquals(1, hooks.pre.get());
        assertEquals(1, hooks.post.get());
        assertEquals(1, toolRuns.get());
    }

    private static StreamingToolScheduler scheduler(ToolRegistry registry, HookRuntime hooks) {
        return new StreamingToolScheduler(registry, ToolSelection.allEnabled(), 1, 1, true,
                new UnknownToolCircuitBreaker().beginAttempt(), AgentEventListener.NOOP,
                () -> { }, () -> { }, null, hooks, new HookContextFactory(Path.of(".")));
    }

    private static Tool tool(String name, ToolRisk risk, AtomicInteger runs, boolean success) {
        return new Tool() {
            @Override public ToolDefinition definition() {
                return new ToolDefinition(name, name, JsonNodeFactory.instance.objectNode(), risk);
            }
            @Override public ToolResult execute(com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                runs.incrementAndGet();
                return success ? ToolResult.success("ok") : ToolResult.failure("business failure");
            }
        };
    }

    private static ToolCall call(String name) {
        return new ToolCall("call-1", name, JsonNodeFactory.instance.objectNode());
    }

    private static final class RecordingHooks implements HookRuntime {
        private final boolean reject;
        private final AtomicInteger pre = new AtomicInteger();
        private final AtomicInteger post = new AtomicInteger();
        private RecordingHooks(boolean reject) { this.reject = reject; }
        @Override public HookRunResult runHooks(HookContext context) {
            if (context.event() == HookEvent.POST_TOOL_USE) post.incrementAndGet();
            return HookRunResult.EMPTY;
        }
        @Override public PreToolHookResult runPreToolHooks(HookContext context) {
            pre.incrementAndGet();
            return reject
                    ? new PreToolHookResult(List.of(), java.util.Optional.of(
                    new ToolRejectedError("test", "policy")))
                    : PreToolHookResult.ALLOW;
        }
        @Override public List<io.imiocode.conversation.SystemReminder> drainPrompts() { return List.of(); }
        @Override public List<HookNotification> drainNotifications() { return List.of(); }
        @Override public void clearPrompts() { }
    }
}
