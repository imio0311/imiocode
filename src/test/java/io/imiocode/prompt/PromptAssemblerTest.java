package io.imiocode.prompt;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.prompt.section.BehaviorSection;
import io.imiocode.prompt.section.CodeQualitySection;
import io.imiocode.prompt.section.IdentitySection;
import io.imiocode.prompt.section.OutputStyleSection;
import io.imiocode.prompt.section.SecuritySection;
import io.imiocode.prompt.section.TaskPatternSection;
import io.imiocode.prompt.section.ToolUsageSection;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptAssemblerTest {

    @Test
    void distributesStablePromptRemindersMessagesAndSelectedTools() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new StubTool("write"));
        registry.register(new StubTool("read"));
        var assembler = new PromptAssembler(defaultBuilder(), registry);
        var request = new ChatRequest(
                List.of(new ChatMessage(MessageRole.USER, "修复测试")),
                List.of(
                        new SystemReminder(ReminderScope.ROUND, "保持只读"),
                        new SystemReminder(ReminderScope.SESSION, "遵守仓库约定"),
                        new SystemReminder(ReminderScope.ENVIRONMENT, "工作区 D:/repo")
                ),
                ToolSelection.only(Set.of("read")),
                OptionalInt.of(2048));

        ApiPayload payload = assembler.assembleApiPayload(request);

        assertTrue(payload.systemPrompt().startsWith("## 身份"));
        assertFalse(payload.systemPrompt().contains("D:/repo"));
        assertFalse(payload.systemPrompt().contains("保持只读"));
        assertEquals(List.of(
                        "<system-reminder>\n工作区 D:/repo\n</system-reminder>",
                        "<system-reminder>\n遵守仓库约定\n</system-reminder>",
                        "修复测试",
                        "<system-reminder>\n保持只读\n</system-reminder>"),
                payload.messages().stream().map(ChatMessage::content).toList());
        assertEquals(List.of("read"),
                payload.tools().stream().map(ToolDefinition::name).toList());
        assertEquals(CacheIntent.stableChannels(), payload.cacheIntent());
        assertEquals(2048, payload.outputTokenLimit().orElseThrow());
    }

    @Test
    void reusesIdenticalSystemPromptWhileDynamicMessagesChange() {
        ToolRegistry registry = new ToolRegistry();
        var assembler = new PromptAssembler(defaultBuilder(), registry);

        ApiPayload first = assembler.assembleApiPayload(new ChatRequest(
                List.of(new ChatMessage(MessageRole.USER, "任务一")),
                List.of(new SystemReminder(ReminderScope.ENVIRONMENT, "时间一"))));
        ApiPayload second = assembler.assembleApiPayload(new ChatRequest(
                List.of(new ChatMessage(MessageRole.USER, "任务二")),
                List.of(new SystemReminder(ReminderScope.ENVIRONMENT, "时间二"))));

        assertEquals(first.systemPrompt(), second.systemPrompt());
        assertFalse(first.messages().equals(second.messages()));
        assertEquals(CacheIntent.systemOnly(), first.cacheIntent());
    }

    @Test
    void usesSystemPromptOverrideAndAllowsEmptyToolSelection() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new StubTool("read"));
        var assembler = new PromptAssembler(defaultBuilder(), registry);
        var request = new ChatRequest(
                List.of(new ChatMessage(MessageRole.USER, "待摘要内容")),
                List.of(),
                ToolSelection.only(Set.of()),
                OptionalInt.of(1024),
                Optional.of("摘要专用提示"));

        ApiPayload payload = assembler.assembleApiPayload(request);

        assertEquals("摘要专用提示", payload.systemPrompt());
        assertTrue(payload.tools().isEmpty());
        assertEquals(CacheIntent.systemOnly(), payload.cacheIntent());
    }

    @Test
    void apiPayloadDefensivelyCopiesCollectionsAndValidatesLimits() {
        List<ChatMessage> mutableMessages = new java.util.ArrayList<>(
                List.of(new ChatMessage(MessageRole.USER, "任务")));
        var payload = new ApiPayload(
                "稳定系统提示",
                mutableMessages,
                List.of(),
                CacheIntent.systemOnly(),
                OptionalInt.empty());
        mutableMessages.add(new ChatMessage(MessageRole.USER, "后加消息"));

        assertEquals(1, payload.messages().size());
        assertThrows(UnsupportedOperationException.class,
                () -> payload.messages().add(
                        new ChatMessage(MessageRole.USER, "不可变")));
        assertThrows(IllegalArgumentException.class,
                () -> new ApiPayload(
                        "系统",
                        List.of(new ChatMessage(MessageRole.USER, "任务")),
                        List.of(),
                        CacheIntent.systemOnly(),
                        OptionalInt.of(0)));
    }

    private static SystemPromptBuilder defaultBuilder() {
        return new SystemPromptBuilder(List.of(
                new IdentitySection(),
                new BehaviorSection(),
                new ToolUsageSection(),
                new CodeQualitySection(),
                new SecuritySection(),
                new TaskPatternSection(),
                new OutputStyleSection()));
    }

    private static final class StubTool implements Tool {
        private final ToolDefinition definition;

        private StubTool(String name) {
            definition = new ToolDefinition(
                    name,
                    "测试工具",
                    JsonNodeFactory.instance.objectNode().put("type", "object"),
                    ToolRisk.LOW);
        }

        @Override
        public ToolDefinition definition() {
            return definition;
        }

        @Override
        public ToolResult execute(ObjectNode arguments) {
            return ToolResult.success("ok");
        }
    }
}
