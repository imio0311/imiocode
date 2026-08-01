package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessagePart;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;

import java.util.List;
import java.util.Objects;

/** 将会话稳定序列化为摘要模型的非指令数据区。 */
public final class ConversationSerializer {
    public String serialize(List<ChatMessage> priorHistory, List<ChatMessage> activeTask) {
        return "<conversation_data>\n<prior_history>\n"
                + serializeMessages(priorHistory)
                + "</prior_history>\n<active_task>\n"
                + serializeMessages(activeTask)
                + "</active_task>\n</conversation_data>";
    }

    private static String serializeMessages(List<ChatMessage> messages) {
        StringBuilder output = new StringBuilder();
        for (ChatMessage message : Objects.requireNonNullElse(messages, List.<ChatMessage>of())) {
            output.append("<message role=\"").append(message.role().name().toLowerCase()).append("\">\n");
            for (MessagePart part : message.parts()) output.append(serializePart(part));
            output.append("</message>\n");
        }
        return output.toString();
    }

    private static String serializePart(MessagePart part) {
        if (part instanceof TextPart text) return tag("text", text.text());
        if (part instanceof ThinkingPart thinking) {
            return tag("thinking", thinking.text()) + tag("thinking_metadata", thinking.metadata().toString());
        }
        if (part instanceof ToolCallPart toolCall) {
            return "<tool_call id=\"" + escape(toolCall.call().id()) + "\" name=\""
                    + escape(toolCall.call().name()) + "\">"
                    + escape(toolCall.call().arguments().toString()) + "</tool_call>\n";
        }
        ToolResultPart toolResult = (ToolResultPart) part;
        return "<tool_result id=\"" + escape(toolResult.callId()) + "\" name=\""
                + escape(toolResult.toolName()) + "\" success=\"" + toolResult.result().success()
                + "\" truncated=\"" + toolResult.result().truncated() + "\" exit_code=\""
                + escape(Objects.toString(toolResult.result().exitCode(), "null")) + "\">\n"
                + tag("output", toolResult.result().output())
                + tag("error", toolResult.result().error())
                + "</tool_result>\n";
    }

    private static String tag(String name, String value) {
        return "<" + name + ">" + escape(value) + "</" + name + ">\n";
    }

    static String escape(String value) {
        return Objects.requireNonNullElse(value, "")
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
