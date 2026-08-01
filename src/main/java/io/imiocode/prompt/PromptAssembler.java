package io.imiocode.prompt;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 将稳定 Prompt、动态消息和当前可用工具分发到统一的三通道载荷。
 */
public final class PromptAssembler {
    private final String systemPrompt;
    private final ToolRegistry toolRegistry;

    public PromptAssembler(
            SystemPromptBuilder systemPromptBuilder,
            ToolRegistry toolRegistry
    ) {
        this.systemPrompt = Objects.requireNonNull(
                systemPromptBuilder, "System Prompt 组装器不能为空").build();
        this.toolRegistry = Objects.requireNonNull(
                toolRegistry, "工具注册中心不能为空");
    }

    public ApiPayload assembleApiPayload(ChatRequest request) {
        ChatRequest checked = Objects.requireNonNull(request, "聊天请求不能为空");
        List<ChatMessage> messages = new ArrayList<>(
                checked.messages().size() + checked.reminders().size());

        appendReminders(messages, checked.reminders(), ReminderScope.ENVIRONMENT);
        appendReminders(messages, checked.reminders(), ReminderScope.SESSION);
        messages.addAll(checked.messages());
        appendReminders(messages, checked.reminders(), ReminderScope.ROUND);

        List<ToolDefinition> tools =
                toolRegistry.enabledDefinitions(checked.toolSelection());
        CacheIntent cacheIntent = tools.isEmpty()
                ? CacheIntent.systemOnly()
                : CacheIntent.stableChannels();
        return new ApiPayload(
                checked.systemPromptOverride().orElse(systemPrompt),
                messages,
                tools,
                cacheIntent,
                checked.outputTokenLimit());
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    private static void appendReminders(
            List<ChatMessage> target,
            List<SystemReminder> reminders,
            ReminderScope scope
    ) {
        for (SystemReminder reminder : reminders) {
            SystemReminder checked = Objects.requireNonNull(
                    reminder, "System Reminder 不能为空");
            if (checked.scope() == scope) {
                target.add(new ChatMessage(
                        MessageRole.USER,
                        checked.wrappedContent()));
            }
        }
    }
}
