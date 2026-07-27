package io.imiocode.agent;

import java.util.List;
import java.util.Objects;

/**
 * 一个可按相同策略执行的连续工具调用批次。
 */
public record ToolBatch(ToolBatchKind kind, List<IndexedToolCall> calls) {
    public ToolBatch {
        kind = Objects.requireNonNull(kind, "kind 不能为空");
        calls = List.copyOf(Objects.requireNonNull(calls, "calls 不能为空"));
        if (calls.isEmpty()) {
            throw new IllegalArgumentException("工具批次不能为空");
        }
        if (kind == ToolBatchKind.SERIAL_BARRIER && calls.size() != 1) {
            throw new IllegalArgumentException("不安全工具批次必须只包含一个调用");
        }
    }
}
