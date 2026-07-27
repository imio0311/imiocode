package io.imiocode.agent;

import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 按模型给出的顺序分批：连续低风险工具并行，其余调用作为串行屏障。
 */
public final class ToolCallPartitioner {
    private final ToolRegistry registry;

    public ToolCallPartitioner(ToolRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry 不能为空");
    }

    public List<ToolBatch> partition(List<ToolCall> calls, ToolSelection selection) {
        Objects.requireNonNull(calls, "calls 不能为空");
        Objects.requireNonNull(selection, "selection 不能为空");
        List<ToolBatch> batches = new ArrayList<>();
        List<IndexedToolCall> safeCalls = new ArrayList<>();

        List<ToolCall> snapshot = List.copyOf(calls);
        for (int index = 0; index < snapshot.size(); index++) {
            ToolCall call = snapshot.get(index);
            IndexedToolCall indexed = new IndexedToolCall(index, call);
            boolean safe = registry.findEnabled(call.name(), selection)
                    .map(tool -> tool.definition().risk() == ToolRisk.LOW)
                    .orElse(false);
            if (safe) {
                safeCalls.add(indexed);
                continue;
            }
            flushSafeBatch(batches, safeCalls);
            batches.add(new ToolBatch(ToolBatchKind.SERIAL_BARRIER, List.of(indexed)));
        }
        flushSafeBatch(batches, safeCalls);
        return List.copyOf(batches);
    }

    public List<ToolBatch> partitionToolCalls(
            List<ToolCall> calls,
            ToolSelection selection
    ) {
        return partition(calls, selection);
    }

    private static void flushSafeBatch(
            List<ToolBatch> batches,
            List<IndexedToolCall> safeCalls
    ) {
        if (!safeCalls.isEmpty()) {
            batches.add(new ToolBatch(ToolBatchKind.PARALLEL_SAFE, List.copyOf(safeCalls)));
            safeCalls.clear();
        }
    }
}
