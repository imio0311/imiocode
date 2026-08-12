package io.imiocode.llm;

import io.imiocode.conversation.ThinkingPart;
import io.imiocode.tool.ToolCall;

import java.util.Objects;

/**
 * Provider 流式响应归一后的事件集合。
 *
 * <p>事件位置用于将交错到达的 Thinking 与 Tool Call 分片重新组装为稳定的消息部件。</p>
 */
public sealed interface LlmEvent permits LlmEvent.TextDelta, LlmEvent.ThinkingDelta,
        LlmEvent.ThinkingCompleted, LlmEvent.ToolCallStarted, LlmEvent.ToolCallDelta,
        LlmEvent.ToolCallCompleted, LlmEvent.StreamCompleted {

    record TextDelta(String text) implements LlmEvent {
        public TextDelta {
            text = requireText(text, "text");
        }
    }

    record ThinkingDelta(int index, String text) implements LlmEvent {
        public ThinkingDelta {
            requireIndex(index);
            text = requireText(text, "text");
        }
    }

    record ThinkingCompleted(int index, ThinkingPart thinking) implements LlmEvent {
        public ThinkingCompleted {
            requireIndex(index);
            Objects.requireNonNull(thinking, "thinking");
        }
    }

    record ToolCallStarted(int index, String id, String name) implements LlmEvent {
        public ToolCallStarted {
            requireIndex(index);
            id = requireText(id, "id");
            name = requireText(name, "name");
        }
    }

    record ToolCallDelta(int index, String jsonFragment) implements LlmEvent {
        public ToolCallDelta {
            requireIndex(index);
            Objects.requireNonNull(jsonFragment, "jsonFragment");
        }
    }

    record ToolCallCompleted(int index, ToolCall call) implements LlmEvent {
        public ToolCallCompleted {
            requireIndex(index);
            Objects.requireNonNull(call, "call");
        }
    }

    record StreamCompleted(TokenUsage usage) implements LlmEvent {
        public StreamCompleted {
            Objects.requireNonNull(usage, "usage");
        }
    }

    private static void requireIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("事件位置不能为负数");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
