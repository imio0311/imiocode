package io.imiocode.prompt;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.tool.ToolDefinition;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** Prompt 组装管线与 Provider 协议适配器之间的不可变边界。 */
public record ApiPayload(
        String systemPrompt,
        List<ChatMessage> messages,
        List<ToolDefinition> tools,
        CacheIntent cacheIntent,
        OptionalInt outputTokenLimit
) {
    public ApiPayload {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("System Prompt 不能为空");
        }
        systemPrompt = systemPrompt.trim();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("消息列表不能为空");
        }
        messages = List.copyOf(messages);
        messages.forEach(message -> Objects.requireNonNull(message, "消息不能为空"));
        tools = tools == null ? List.of() : List.copyOf(tools);
        tools.forEach(tool -> Objects.requireNonNull(tool, "工具定义不能为空"));
        cacheIntent = Objects.requireNonNull(cacheIntent, "缓存意图不能为空");
        outputTokenLimit = outputTokenLimit == null
                ? OptionalInt.empty()
                : outputTokenLimit;
        if (outputTokenLimit.isPresent() && outputTokenLimit.getAsInt() <= 0) {
            throw new IllegalArgumentException("输出 Token 上限必须为正数");
        }
    }
}
