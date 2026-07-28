package io.imiocode.agent;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamingToolSchedulerTest {
    @Test
    void startsOnlyContinuousLowPrefix() throws Exception {
        ToolRegistry registry = new ToolRegistry();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        registry.register(blockingTool("low0", ToolRisk.LOW, started, release, running, maximum));
        registry.register(blockingTool("low1", ToolRisk.LOW, started, release, running, maximum));

        var attempt = new UnknownToolCircuitBreaker().beginAttempt();
        try (StreamingToolScheduler scheduler = scheduler(registry, attempt, 2)) {
            scheduler.onToolCallCompleted(0, call("0", "low0"));
            scheduler.onToolCallCompleted(1, call("1", "low1"));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertTrue(scheduler.toolsStarted());
            assertEquals(2, maximum.get());
            release.countDown();
            scheduler.onStreamCompleted();
            assertEquals(2, scheduler.awaitResults().size());
        }
    }

    @Test
    void preservesLowWriteLowBarrier() throws Exception {
        ToolRegistry registry = new ToolRegistry();
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        registry.register(recordingTool(
                "low0", ToolRisk.LOW, order, firstStarted, releaseFirst));
        registry.register(recordingTool(
                "write", ToolRisk.HIGH, order, new CountDownLatch(0), new CountDownLatch(0)));
        registry.register(recordingTool(
                "low2", ToolRisk.LOW, order, new CountDownLatch(0), new CountDownLatch(0)));

        try (StreamingToolScheduler scheduler = scheduler(
                registry, new UnknownToolCircuitBreaker().beginAttempt(), 2)) {
            scheduler.onToolCallCompleted(0, call("0", "low0"));
            scheduler.onToolCallCompleted(1, call("1", "write"));
            scheduler.onToolCallCompleted(2, call("2", "low2"));
            assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
            assertFalse(order.contains("write-start"));
            assertFalse(order.contains("low2-start"));
            releaseFirst.countDown();
            scheduler.onStreamCompleted();
            scheduler.awaitResults();
        }
        assertEquals(List.of(
                "low0-start", "low0-end",
                "write-start", "write-end",
                "low2-start", "low2-end"), order);
    }

    private static StreamingToolScheduler scheduler(
            ToolRegistry registry,
            UnknownToolCircuitBreaker.Attempt attempt,
            int parallelism
    ) {
        return new StreamingToolScheduler(
                registry,
                ToolSelection.allEnabled(),
                1,
                parallelism,
                true,
                attempt,
                AgentEventListener.NOOP,
                () -> {
                },
                () -> {
                });
    }

    private static Tool blockingTool(
            String name,
            ToolRisk risk,
            CountDownLatch started,
            CountDownLatch release,
            AtomicInteger running,
            AtomicInteger maximum
    ) {
        return tool(name, risk, arguments -> {
            int active = running.incrementAndGet();
            maximum.accumulateAndGet(active, Math::max);
            started.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    return ToolResult.failure("等待超时");
                }
                return ToolResult.success(name);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return ToolResult.interrupted("", "中断", false);
            } finally {
                running.decrementAndGet();
            }
        });
    }

    private static Tool recordingTool(
            String name,
            ToolRisk risk,
            List<String> order,
            CountDownLatch started,
            CountDownLatch release
    ) {
        return tool(name, risk, arguments -> {
            order.add(name + "-start");
            started.countDown();
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            order.add(name + "-end");
            return ToolResult.success(name);
        });
    }

    private static Tool tool(
            String name,
            ToolRisk risk,
            java.util.function.Function<ObjectNode, ToolResult> execution
    ) {
        return new Tool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(
                        name, name, JsonNodeFactory.instance.objectNode(), risk);
            }

            @Override
            public ToolResult execute(ObjectNode arguments) {
                return execution.apply(arguments);
            }
        };
    }

    private static ToolCall call(String id, String name) {
        return new ToolCall(id, name, JsonNodeFactory.instance.objectNode());
    }
}
