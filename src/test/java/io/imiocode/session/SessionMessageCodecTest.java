package io.imiocode.session;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.conversation.AnthropicThinkingMetadata;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.DeepSeekReasoningMetadata;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.OpenAiReasoningMetadata;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionMessageCodecTest {
    private final SessionMessageCodec codec = new SessionMessageCodec();

    @Test
    void roundTripsEveryPartAndMetadataKind() {
        var args = JsonNodeFactory.instance.objectNode().put("path", "你好.java");
        List<ChatMessage> messages = List.of(
                new ChatMessage(MessageRole.USER, "读取文件"),
                new ChatMessage(MessageRole.ASSISTANT, List.of(
                        new ThinkingPart("分析", new AnthropicThinkingMetadata("sig", "")),
                        new ThinkingPart("推理", new OpenAiReasoningMetadata("item", "encrypted")),
                        new ThinkingPart("思考", new DeepSeekReasoningMetadata()),
                        new TextPart("开始"),
                        new ToolCallPart(new ToolCall("call-1", "read_file", args)))),
                new ChatMessage(MessageRole.TOOL, List.of(new ToolResultPart(
                        "call-1", "read_file",
                        new ToolResult(false, "partial", "failed", true, Duration.ofMillis(15), 2)))));

        List<ChatMessage> restored = messages.stream().map(codec::encode).map(codec::decode).toList();

        assertEquals(messages, restored);
        codec.validateChain(restored);
    }

    @Test
    void rejectsOrphanToolResult() {
        var result = new ChatMessage(MessageRole.TOOL, List.of(new ToolResultPart(
                "missing", "read_file", ToolResult.success("ok"))));
        assertThrows(SessionException.class, () -> codec.validateChain(List.of(result)));
    }

    @Test
    void rejectsToolCallWithoutResult() {
        var args = JsonNodeFactory.instance.objectNode().put("path", "pom.xml");
        var call = new ChatMessage(MessageRole.ASSISTANT, List.of(
                new ToolCallPart(new ToolCall("unfinished", "read_file", args))));

        assertThrows(SessionException.class, () -> codec.validateChain(List.of(call)));
    }
}
