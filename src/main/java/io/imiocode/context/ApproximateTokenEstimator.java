package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.prompt.ApiPayload;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolResult;

import java.util.List;
import java.util.Objects;

/** 统一、确定性的近似 Token 估算器。 */
public final class ApproximateTokenEstimator {
    private static final long MESSAGE_OVERHEAD_CHARS = 16;
    private static final long PART_OVERHEAD_CHARS = 12;
    private static final long TOOL_OVERHEAD_CHARS = 32;

    public long estimate(ApiPayload payload, int defaultOutputTokens) {
        Objects.requireNonNull(payload, "payload");
        if (defaultOutputTokens <= 0) throw new IllegalArgumentException("defaultOutputTokens 必须为正数");
        long chars = payload.systemPrompt().length();
        for (ChatMessage message : payload.messages()) {
            chars = add(chars, MESSAGE_OVERHEAD_CHARS + message.role().name().length());
            for (MessagePart part : message.parts()) chars = add(chars, estimatePartChars(part));
        }
        for (ToolDefinition tool : payload.tools()) {
            chars = add(chars, TOOL_OVERHEAD_CHARS + tool.name().length() + tool.description().length());
            chars = add(chars, tool.inputSchema().toString().length());
            chars = add(chars, tool.risk().name().length());
        }
        long inputTokens = (long) Math.ceil(chars / ContextPolicy.CHARACTERS_PER_TOKEN);
        return add(inputTokens, payload.outputTokenLimit().orElse(defaultOutputTokens));
    }

    /** 只估算会话历史，不组装 Prompt、工具 Schema，也不包含输出预算。 */
    public long estimateMessages(List<ChatMessage> messages) {
        Objects.requireNonNull(messages, "messages");
        long chars = 0;
        for (ChatMessage message : messages) {
            chars = add(chars, MESSAGE_OVERHEAD_CHARS + message.role().name().length());
            for (MessagePart part : message.parts()) chars = add(chars, estimatePartChars(part));
        }
        return (long) Math.ceil(chars / ContextPolicy.CHARACTERS_PER_TOKEN);
    }

    private static long estimatePartChars(MessagePart part) {
        long chars = PART_OVERHEAD_CHARS + part.getClass().getSimpleName().length();
        if (part instanceof TextPart text) return add(chars, text.text().length());
        if (part instanceof ThinkingPart thinking) return add(add(chars, thinking.text().length()), thinking.metadata().toString().length());
        if (part instanceof ToolCallPart toolCall) {
            chars = add(chars, toolCall.call().id().length() + toolCall.call().name().length());
            return add(chars, toolCall.call().arguments().toString().length());
        }
        ToolResultPart toolResult = (ToolResultPart) part;
        ToolResult result = toolResult.result();
        chars = add(chars, toolResult.callId().length() + toolResult.toolName().length());
        chars = add(chars, result.output().length() + result.error().length());
        chars = add(chars, result.duration().toString().length());
        chars = add(chars, result.exitCode() == null ? 4 : result.exitCode().toString().length());
        return add(chars, 16);
    }

    static long add(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
}
