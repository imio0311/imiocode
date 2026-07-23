package io.imiocode.tool;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutorTest {
    @Test
    void executesSeriallyAndContinuesAfterFailure() {
        List<String> order = new ArrayList<>();
        ToolRegistry registry = new ToolRegistry();
        registry.register(new RecordingTool("first", true, order));
        registry.register(new RecordingTool("second", false, order));
        ToolExecutor executor = new ToolExecutor(registry);
        List<ToolExecutionState> states = new ArrayList<>();

        List<ToolExecution> results = executor.executeAll(
                List.of(call("1", "first"), call("2", "second"), call("3", "missing")),
                event -> states.add(event.state()));

        assertEquals(List.of("first", "second"), order);
        assertEquals(3, results.size());
        assertTrue(results.get(0).result().success());
        assertFalse(results.get(1).result().success());
        assertFalse(results.get(2).result().success());
        assertEquals(List.of(
                ToolExecutionState.QUEUED, ToolExecutionState.RUNNING, ToolExecutionState.SUCCEEDED,
                ToolExecutionState.QUEUED, ToolExecutionState.RUNNING, ToolExecutionState.FAILED,
                ToolExecutionState.QUEUED, ToolExecutionState.FAILED), states);
    }

    @Test
    void cancellationStopsActiveAndPendingTools() throws Exception {
        ToolRegistry registry = new ToolRegistry();
        BlockingTool blocking = new BlockingTool();
        AtomicInteger pendingRuns = new AtomicInteger();
        registry.register(blocking);
        registry.register(new CountingTool(pendingRuns));
        ToolExecutor executor = new ToolExecutor(registry);

        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var future = threads.submit(() -> executor.executeAll(
                    List.of(call("1", "blocking"), call("2", "pending")),
                    ToolExecutionListener.NOOP));
            assertTrue(blocking.started.await(2, TimeUnit.SECONDS));
            executor.cancel();
            future.get(2, TimeUnit.SECONDS);
        }

        assertTrue(blocking.cancelled);
        assertEquals(0, pendingRuns.get());
    }

    private static ToolCall call(String id, String name) {
        return new ToolCall(id, name, JsonNodeFactory.instance.objectNode());
    }

    private static ToolDefinition definition(String name) {
        return new ToolDefinition(
                name,
                "测试工具",
                JsonNodeFactory.instance.objectNode().put("type", "object"),
                ToolRisk.LOW);
    }

    private record RecordingTool(
            String name,
            boolean succeeds,
            List<String> order) implements Tool {
        @Override
        public ToolDefinition definition() {
            return ToolExecutorTest.definition(name);
        }

        @Override
        public ToolResult execute(ObjectNode arguments) {
            order.add(name);
            return succeeds ? ToolResult.success(name) : ToolResult.failure(name);
        }
    }

    private static final class BlockingTool implements Tool {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private volatile boolean cancelled;

        @Override
        public ToolDefinition definition() {
            return ToolExecutorTest.definition("blocking");
        }

        @Override
        public ToolResult execute(ObjectNode arguments) {
            started.countDown();
            try {
                release.await();
                return ToolResult.interrupted("", "cancelled", false);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return ToolResult.interrupted("", "cancelled", false);
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            release.countDown();
        }
    }

    private record CountingTool(AtomicInteger runs) implements Tool {
        @Override
        public ToolDefinition definition() {
            return ToolExecutorTest.definition("pending");
        }

        @Override
        public ToolResult execute(ObjectNode arguments) {
            runs.incrementAndGet();
            return ToolResult.success("pending");
        }
    }
}
