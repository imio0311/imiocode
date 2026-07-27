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

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolCallPartitionerTest {
    @Test
    void preservesOrderAcrossSafeRunsAndSerialBarriers() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("read", ToolRisk.LOW));
        registry.register(tool("grep", ToolRisk.LOW));
        registry.register(tool("write", ToolRisk.MEDIUM));
        registry.register(tool("bash", ToolRisk.HIGH));

        List<ToolBatch> batches = new ToolCallPartitioner(registry).partitionToolCalls(
                List.of(
                        call("1", "read"),
                        call("2", "grep"),
                        call("3", "write"),
                        call("4", "read"),
                        call("5", "bash"),
                        call("6", "missing")
                ),
                ToolSelection.allEnabled()
        );

        assertEquals(List.of(
                ToolBatchKind.PARALLEL_SAFE,
                ToolBatchKind.SERIAL_BARRIER,
                ToolBatchKind.PARALLEL_SAFE,
                ToolBatchKind.SERIAL_BARRIER,
                ToolBatchKind.SERIAL_BARRIER
        ), batches.stream().map(ToolBatch::kind).toList());
        assertEquals(List.of(0, 1, 2, 3, 4, 5), batches.stream()
                .flatMap(batch -> batch.calls().stream())
                .map(IndexedToolCall::originalIndex)
                .toList());
    }

    @Test
    void planSelectionTurnsDisallowedToolIntoBarrier() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("read", ToolRisk.LOW));
        registry.register(tool("write", ToolRisk.LOW));

        List<ToolBatch> batches = new ToolCallPartitioner(registry).partition(
                List.of(call("1", "read"), call("2", "write")),
                ToolSelection.only(Set.of("read")));

        assertEquals(ToolBatchKind.PARALLEL_SAFE, batches.get(0).kind());
        assertEquals(ToolBatchKind.SERIAL_BARRIER, batches.get(1).kind());
    }

    private static ToolCall call(String id, String name) {
        return new ToolCall(id, name, JsonNodeFactory.instance.objectNode());
    }

    private static Tool tool(String name, ToolRisk risk) {
        return new Tool() {
            @Override
            public ToolDefinition definition() {
                return new ToolDefinition(
                        name,
                        "测试工具",
                        JsonNodeFactory.instance.objectNode().put("type", "object"),
                        risk
                );
            }

            @Override
            public ToolResult execute(ObjectNode arguments) {
                return ToolResult.success(name);
            }
        };
    }
}
