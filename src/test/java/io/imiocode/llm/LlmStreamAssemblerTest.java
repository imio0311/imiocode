package io.imiocode.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.conversation.DeepSeekReasoningMetadata;
import io.imiocode.conversation.ThinkingPart;
import io.imiocode.conversation.ToolCallPart;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LlmStreamAssemblerTest {
    @Test
    void emitsOrderedEventsAndBuildsStructuredResponse() throws Exception {
        List<LlmEvent> events = new ArrayList<>();
        LlmStreamAssembler assembler = new LlmStreamAssembler(new ObjectMapper(), events::add);

        assembler.startThinking(0);
        assembler.appendThinking(0, "分析");
        assembler.completeThinking(0, new DeepSeekReasoningMetadata());
        assembler.startTool(1, "call-1", "read_file");
        assembler.appendToolArguments(1, "{\"path\":\"README.md\"}");
        assembler.completeTool(1);
        assembler.emitText("完成");
        TokenUsage usage = new TokenUsageBuilder().input(10).output(5).reasoning(2).build();

        var response = assembler.complete(usage);

        assertEquals(List.of(
                        LlmEvent.ThinkingDelta.class,
                        LlmEvent.ThinkingCompleted.class,
                        LlmEvent.ToolCallStarted.class,
                        LlmEvent.ToolCallDelta.class,
                        LlmEvent.ToolCallCompleted.class,
                        LlmEvent.TextDelta.class,
                        LlmEvent.StreamCompleted.class),
                events.stream().map(Object::getClass).toList());
        assertInstanceOf(ThinkingPart.class, response.message().parts().get(0));
        ToolCallPart toolPart = assertInstanceOf(ToolCallPart.class, response.message().parts().get(1));
        assertEquals("README.md", toolPart.call().arguments().path("path").asText());
        assertEquals("完成", response.text());
        assertEquals(usage, response.usage());
    }

    @Test
    void rejectsIncompleteThinkingWithoutPublishingCompletion() throws Exception {
        List<LlmEvent> events = new ArrayList<>();
        LlmStreamAssembler assembler = new LlmStreamAssembler(new ObjectMapper(), events::add);
        assembler.startThinking(0);
        assembler.appendThinking(0, "未结束");

        assertThrows(LlmException.class, () -> assembler.complete(TokenUsage.unknown()));

        assertFalse(events.stream().anyMatch(LlmEvent.StreamCompleted.class::isInstance));
    }

    @Test
    void rejectsInvalidToolArgumentsWithoutPublishingToolCompletion() throws Exception {
        List<LlmEvent> events = new ArrayList<>();
        LlmStreamAssembler assembler = new LlmStreamAssembler(new ObjectMapper(), events::add);
        assembler.startTool(0, "call-1", "read_file");
        assembler.appendToolArguments(0, "{");

        assertThrows(LlmException.class, () -> assembler.completeTool(0));

        assertFalse(events.stream().anyMatch(LlmEvent.ToolCallCompleted.class::isInstance));
    }

    @Test
    void rejectsEmptyResponseAndDistinguishesUnknownFromZeroUsage() {
        List<LlmEvent> events = new ArrayList<>();
        LlmStreamAssembler assembler = new LlmStreamAssembler(new ObjectMapper(), events::add);

        assertThrows(LlmException.class, () -> assembler.complete(TokenUsage.unknown()));
        assertFalse(events.stream().anyMatch(LlmEvent.StreamCompleted.class::isInstance));

        TokenUsage zeroOutput = new TokenUsageBuilder().output(0).build();
        assertEquals(OptionalLong.empty(), zeroOutput.inputTokens());
        assertEquals(OptionalLong.of(0), zeroOutput.outputTokens());
    }
}
