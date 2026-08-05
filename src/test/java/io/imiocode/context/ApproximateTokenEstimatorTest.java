package io.imiocode.context;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.DeepSeekReasoningMetadata;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.TextPart;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.conversation.ToolCallPart;
import io.imiocode.conversation.ToolResultPart;
import io.imiocode.prompt.ApiPayload;
import io.imiocode.prompt.CacheIntent;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApproximateTokenEstimatorTest {
    private final ApproximateTokenEstimator estimator = new ApproximateTokenEstimator();

    @Test
    void estimatesAllMessagePartsToolsAndOutputReserveDeterministically() {
        var arguments = JsonNodeFactory.instance.objectNode().put("path", "中文/😀.txt");
        var schema = JsonNodeFactory.instance.objectNode().put("type", "object");
        var payload = new ApiPayload(
                "稳定系统提示",
                List.of(
                        new ChatMessage(MessageRole.USER, "读取 文件"),
                        new ChatMessage(MessageRole.ASSISTANT, List.of(
                                new ThinkingPart("分析", new DeepSeekReasoningMetadata()),
                                new TextPart("准备调用"),
                                new ToolCallPart(new ToolCall("call-1", "read_file", arguments)))),
                        new ChatMessage(MessageRole.TOOL, List.of(
                                new ToolResultPart("call-1", "read_file", ToolResult.success("结果😀"))))),
                List.of(new ToolDefinition("read_file", "读取文件", schema, ToolRisk.LOW)),
                CacheIntent.stableChannels(),
                OptionalInt.of(2048));

        long first = estimator.estimate(payload, 4096);
        long second = estimator.estimate(payload, 4096);
        assertEquals(first, second);
        assertTrue(first > 2048);
    }

    @Test
    void largerChannelsAndOutputReserveIncreaseEstimate() {
        ApiPayload small = payload("系统", "短", OptionalInt.empty());
        ApiPayload large = payload("系统内容更长", "这是一段更长的中文消息😀", OptionalInt.of(8192));
        assertTrue(estimator.estimate(large, 4096) > estimator.estimate(small, 4096));
    }

    @Test
    void saturatedAdditionNeverWrapsNegative() {
        assertEquals(Long.MAX_VALUE, ApproximateTokenEstimator.add(Long.MAX_VALUE - 2, 10));
    }

    @Test
    void estimatesHistoryOnlyWithoutOutputOrToolBudget() {
        List<ChatMessage> history = List.of(
                new ChatMessage(MessageRole.USER, "一段会话历史"),
                new ChatMessage(MessageRole.ASSISTANT, "回答"));

        assertEquals(0, estimator.estimateMessages(List.of()));
        assertTrue(estimator.estimateMessages(history) > 0);
        assertEquals(estimator.estimateMessages(history), estimator.estimateMessages(history));
    }

    private static ApiPayload payload(String system, String message, OptionalInt limit) {
        return new ApiPayload(system, List.of(new ChatMessage(MessageRole.USER, message)),
                List.of(), CacheIntent.systemOnly(), limit);
    }
}
