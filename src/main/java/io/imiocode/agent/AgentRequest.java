package io.imiocode.agent;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.skill.SkillInvocation;
import io.imiocode.tool.ToolSelection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 一次 Agent 任务的输入。
 */
public record AgentRequest(
        List<ChatMessage> committedHistory,
        ChatMessage userMessage,
        List<SystemReminder> reminders,
        Optional<SkillInvocation> skillInvocation,
        Optional<ToolSelection> toolSelection
) {
    public AgentRequest {
        committedHistory = List.copyOf(Objects.requireNonNull(committedHistory, "committedHistory 不能为空"));
        userMessage = Objects.requireNonNull(userMessage, "userMessage 不能为空");
        reminders = List.copyOf(Objects.requireNonNull(reminders, "reminders 不能为空"));
        skillInvocation = skillInvocation == null ? Optional.empty() : skillInvocation;
        toolSelection = toolSelection == null ? Optional.empty() : toolSelection;
        if (userMessage.role() != MessageRole.USER) {
            throw new IllegalArgumentException("userMessage 必须是 user 消息");
        }
    }

    public AgentRequest(ChatMessage userMessage) {
        this(List.of(), userMessage, List.of(), Optional.empty(), Optional.empty());
    }

    public AgentRequest(List<ChatMessage> committedHistory, ChatMessage userMessage,
                        List<SystemReminder> reminders) {
        this(committedHistory, userMessage, reminders, Optional.empty(), Optional.empty());
    }

    public AgentRequest(List<ChatMessage> committedHistory, ChatMessage userMessage,
                        List<SystemReminder> reminders, Optional<SkillInvocation> skillInvocation) {
        this(committedHistory, userMessage, reminders, skillInvocation, Optional.empty());
    }

    public AgentRequest(List<ChatMessage> committedHistory, ChatMessage userMessage,
                        List<SystemReminder> reminders, ToolSelection toolSelection) {
        this(committedHistory, userMessage, reminders, Optional.empty(), Optional.of(toolSelection));
    }
}
