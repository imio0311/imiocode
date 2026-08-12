package io.imiocode.conversation;

import io.imiocode.tool.ToolSelection;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 一次 Provider 调用所需的不可变消息、提醒、工具选择和可选覆盖项。
 *
 * <p>System Prompt 覆盖主要供受控子 Agent 场景使用；普通对话由 PromptAssembler 统一构建。</p>
 */
public record ChatRequest(
        List<ChatMessage> messages,
        List<SystemReminder> reminders,
        ToolSelection toolSelection,
        OptionalInt outputTokenLimit,
        Optional<String> systemPromptOverride) {
    public ChatRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("消息列表不能为空");
        }
        messages = List.copyOf(messages);
        reminders = reminders == null ? List.of() : List.copyOf(reminders);
        toolSelection = toolSelection == null ? ToolSelection.allEnabled() : toolSelection;
        outputTokenLimit = outputTokenLimit == null ? OptionalInt.empty() : outputTokenLimit;
        systemPromptOverride = systemPromptOverride == null ? Optional.empty() : systemPromptOverride;
        if (outputTokenLimit.isPresent() && outputTokenLimit.getAsInt() <= 0) {
            throw new IllegalArgumentException("outputTokenLimit 必须为正数");
        }
        systemPromptOverride = systemPromptOverride.map(value -> {
            if (value.isBlank()) {
                throw new IllegalArgumentException("systemPromptOverride 不能为空白文本");
            }
            return value.trim();
        });
    }

    public ChatRequest(
            List<ChatMessage> messages,
            List<SystemReminder> reminders,
            ToolSelection toolSelection,
            OptionalInt outputTokenLimit) {
        this(messages, reminders, toolSelection, outputTokenLimit, Optional.empty());
    }

    public ChatRequest(
            List<ChatMessage> messages,
            List<SystemReminder> reminders,
            ToolSelection toolSelection
    ) {
        this(messages, reminders, toolSelection, OptionalInt.empty(), Optional.empty());
    }

    public ChatRequest(List<ChatMessage> messages, List<SystemReminder> reminders) {
        this(messages, reminders, ToolSelection.allEnabled(), OptionalInt.empty(), Optional.empty());
    }

    public ChatRequest(List<ChatMessage> messages) {
        this(messages, List.of(), ToolSelection.allEnabled(), OptionalInt.empty(), Optional.empty());
    }
}
