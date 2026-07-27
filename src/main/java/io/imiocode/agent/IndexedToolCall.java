package io.imiocode.agent;

import io.imiocode.tool.ToolCall;

import java.util.Objects;

/**
 * 保留模型原始顺序的工具调用。
 */
public record IndexedToolCall(int originalIndex, ToolCall call) {
    public IndexedToolCall {
        if (originalIndex < 0) {
            throw new IllegalArgumentException("originalIndex 不能为负数");
        }
        call = Objects.requireNonNull(call, "call 不能为空");
    }
}
