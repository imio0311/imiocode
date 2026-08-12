package io.imiocode.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.conversation.AnthropicThinkingMetadata;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.DeepSeekReasoningMetadata;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.OpenAiReasoningMetadata;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.session.record.StoredMessage;
import io.imiocode.session.record.StoredPart;
import io.imiocode.session.record.StoredToolResult;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 显式映射领域消息，避免持久化 Java 多态类名。 */
public final class SessionMessageCodec {
    public StoredMessage encode(ChatMessage message) {
        List<StoredPart> parts = message.parts().stream().map(this::encodePart).toList();
        return new StoredMessage(message.role().name().toLowerCase(Locale.ROOT), parts);
    }

    public ChatMessage decode(StoredMessage stored) {
        if (stored == null || stored.role() == null || stored.parts().isEmpty()) {
            throw new SessionException("存储消息不完整");
        }
        MessageRole role;
        try { role = MessageRole.valueOf(stored.role().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new SessionException("未知消息角色", exception); }
        List<MessagePart> parts = stored.parts().stream().map(this::decodePart).toList();
        try { return new ChatMessage(role, parts); }
        catch (IllegalArgumentException exception) { throw new SessionException("消息角色与内容不匹配", exception); }
    }

    public void validateChain(List<ChatMessage> messages) {
        Set<String> pendingCalls = new HashSet<>();
        for (ChatMessage message : messages) {
            for (MessagePart part : message.parts()) {
                if (part instanceof ToolCallPart toolCall) {
                    if (!pendingCalls.add(toolCall.call().id())) {
                        throw new SessionException("尚未完成的工具调用 ID 重复");
                    }
                } else if (part instanceof ToolResultPart result) {
                    if (!pendingCalls.remove(result.callId())) {
                        throw new SessionException("工具结果没有对应调用或存在重复结果");
                    }
                }
            }
        }
        if (!pendingCalls.isEmpty()) throw new SessionException("工具调用缺少对应结果");
    }

    private StoredPart encodePart(MessagePart part) {
        if (part instanceof TextPart text) {
            return new StoredPart("text", text.text(), null, null, null, null, null);
        }
        if (part instanceof ThinkingPart thinking) {
            ObjectNode metadata = JsonNodeFactory.instance.objectNode();
            if (thinking.metadata() instanceof AnthropicThinkingMetadata anthropic) {
                metadata.put("provider", "anthropic");
                metadata.put("signature", anthropic.signature());
                metadata.put("redactedData", anthropic.redactedData());
            } else if (thinking.metadata() instanceof OpenAiReasoningMetadata openAi) {
                metadata.put("provider", "openai");
                metadata.put("itemId", openAi.itemId());
                metadata.put("encryptedContent", openAi.encryptedContent());
            } else if (thinking.metadata() instanceof DeepSeekReasoningMetadata) {
                metadata.put("provider", "deepseek");
            } else {
                throw new SessionException("未知思考元数据");
            }
            return new StoredPart("thinking", thinking.text(), metadata, null, null, null, null);
        }
        if (part instanceof ToolCallPart tool) {
            return new StoredPart("tool_call", null, null, tool.call().id(), tool.call().name(),
                    tool.call().arguments(), null);
        }
        if (part instanceof ToolResultPart tool) {
            ToolResult result = tool.result();
            StoredToolResult storedResult = new StoredToolResult(result.success(), result.output(), result.error(),
                    result.truncated(), result.duration().toMillis(), result.exitCode());
            return new StoredPart("tool_result", null, null, tool.callId(), tool.toolName(), null, storedResult);
        }
        throw new SessionException("未知消息部件");
    }

    private MessagePart decodePart(StoredPart part) {
        if (part == null || part.type() == null) throw new SessionException("存储消息部件不完整");
        return switch (part.type()) {
            case "text" -> new TextPart(required(part.text(), "text"));
            case "thinking" -> new ThinkingPart(part.text() == null ? "" : part.text(), decodeThinking(part.metadata()));
            case "tool_call" -> new ToolCallPart(new ToolCall(required(part.callId(), "callId"),
                    required(part.toolName(), "toolName"), requireObject(part.arguments())));
            case "tool_result" -> decodeToolResult(part);
            default -> throw new SessionException("未知消息部件类型");
        };
    }

    private static ToolResultPart decodeToolResult(StoredPart part) {
        StoredToolResult stored = part.toolResult();
        if (stored == null || stored.durationMillis() < 0) throw new SessionException("工具结果不完整");
        ToolResult result = new ToolResult(stored.success(), stored.output(), stored.error(), stored.truncated(),
                Duration.ofMillis(stored.durationMillis()), stored.exitCode());
        return new ToolResultPart(required(part.callId(), "callId"), required(part.toolName(), "toolName"), result);
    }

    private static io.imiocode.conversation.ThinkingMetadata decodeThinking(JsonNode metadata) {
        if (metadata == null || !metadata.isObject()) throw new SessionException("思考元数据不完整");
        String provider = requiredText(metadata, "provider");
        return switch (provider) {
            case "anthropic" -> new AnthropicThinkingMetadata(text(metadata, "signature"), text(metadata, "redactedData"));
            case "openai" -> new OpenAiReasoningMetadata(requiredText(metadata, "itemId"), text(metadata, "encryptedContent"));
            case "deepseek" -> new DeepSeekReasoningMetadata();
            default -> throw new SessionException("未知思考 Provider");
        };
    }

    private static ObjectNode requireObject(JsonNode node) {
        if (!(node instanceof ObjectNode object)) throw new SessionException("工具参数必须是 JSON 对象");
        return object.deepCopy();
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new SessionException(name + " 不能为空");
        return value;
    }

    private static String requiredText(JsonNode node, String field) {
        String value = text(node, field);
        if (value.isBlank()) throw new SessionException("思考元数据缺少 " + field);
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
