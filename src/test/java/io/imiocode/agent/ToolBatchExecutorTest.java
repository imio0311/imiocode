package io.imiocode.agent;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolExecution;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolBatchExecutorTest {
    @Test
    void runsSafeToolsConcurrentlyButReturnsOriginalOrder() {
        ToolRegistry registry = new ToolRegistry();
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        registry.register(blockingTool("slow", bothStarted, release, 80));
        registry.register(blockingTool("fast", bothStarted, release, 0));

        List<ToolExecution> results;
        try (ToolBatchExecutor executor = new ToolBatchExecutor(registry, 2)) {
            Thread releaser = Thread.startVirtualThread(() -> {
                try {
                    assertTrue(bothStarted.await(2, TimeUnit.SECONDS));
                    release.countDown();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
            results = executor.execute(
                    List.of(call("1", "slow"), call("2", "fast")),
                    ToolSelection.allEnabled(),
                    1,
                    AgentEventListener.NOOP
            );
            try {
                releaser.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

        assertEquals(List.of("slow", "fast"),
                results.stream().map(execution -> execution.call().name()).toList());
    }

    @Test
    void respectsParallelLimitAndDoesNotExecuteDisallowedTool() {
        ToolRegistry registry = new ToolRegistry();
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        AtomicInteger forbiddenRuns = new AtomicInteger();
        for (int index = 0; index < 4; index++) {
            registry.register(countingTool("safe" + index, active, maximum));
        }
        registry.register(countingTool("forbidden", forbiddenRuns, new AtomicInteger()));

        List<ToolCall> calls = List.of(
                call("0", "safe0"),
                call("1", "safe1"),
                call("2", "safe2"),
                call("3", "safe3"),
                call("4", "forbidden")
        );
        List<ToolExecution> results;
        try (ToolBatchExecutor executor = new ToolBatchExecutor(registry, 2)) {
            results = executor.execute(
                    calls,
                    ToolSelection.only(SetSupport.names("safe0", "safe1", "safe2", "safe3")),
                    1,
                    AgentEventListener.NOOP
            );
        }

        assertTrue(maximum.get() <= 2);
        assertEquals(0, forbiddenRuns.get());
        assertEquals(5, results.size());
        assertTrue(!results.getLast().result().success());
    }

    private static Tool blockingTool(
            String name,
            CountDownLatch started,
            CountDownLatch release,
            long delayMillis
    ) {
        return new Tool() {
            @Override
            public ToolDefinition definition() {
                return ToolBatchExecutorTest.definition(name);
            }

            @Override
            public ToolResult execute(ObjectNode arguments) {
                started.countDown();
                try {
                    release.await();
                    Thread.sleep(delayMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                return ToolResult.success(name);
            }
        };
    }

    private static Tool countingTool(
            String name,
            AtomicInteger active,
            AtomicInteger maximum
    ) {
        return new Tool() {
            @Override
            public ToolDefinition definition() {
                return ToolBatchExecutorTest.definition(name);
            }

            @Override
            public ToolResult execute(ObjectNode arguments) {
                int current = active.incrementAndGet();
                maximum.accumulateAndGet(current, Math::max);
                try {
                    Thread.sleep(30);
                    return ToolResult.success(name);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return ToolResult.interrupted("", "中断", false);
                } finally {
                    active.decrementAndGet();
                }
            }
        };
    }

    private static ToolCall call(String id, String name) {
        return new ToolCall(id, name, JsonNodeFactory.instance.objectNode());
    }

    private static ToolDefinition definition(String name) {
        return new ToolDefinition(
                name,
                "测试工具",
                JsonNodeFactory.instance.objectNode().put("type", "object"),
                ToolRisk.LOW
        );
    }

    private static final class SetSupport {
        private SetSupport() {
        }

        private static java.util.Set<String> names(String... names) {
            return java.util.Set.of(names);
        }
    }
}
