package io.imiocode.subagent.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.subagent.runtime.SubagentRunMode;

import java.util.ArrayList;
import java.util.List;

/** 构造 Fork 上下文；完整历史保持字节顺序不变以复用 Prompt Cache。 */
public final class SubagentContextBuilder {
    public List<ChatMessage> fullHistory(List<ChatMessage> parentHistory) {
        return List.copyOf(parentHistory == null ? List.of() : parentHistory);
    }

    public List<ChatMessage> history(AgentDefinition definition, List<ChatMessage> parentHistory,
                                     SubagentRunMode mode) {
        List<ChatMessage> result = mode == SubagentRunMode.FORK
                ? new ArrayList<>(fullHistory(parentHistory)) : new ArrayList<>();
        if (mode == SubagentRunMode.DEFINITION) {
            result.add(new ChatMessage(MessageRole.USER, new SystemReminder(definition.prompt()).wrappedContent()));
            definition.initialPrompt().ifPresent(value -> result.add(
                    new ChatMessage(MessageRole.USER, new SystemReminder(value).wrappedContent())));
        } else {
            result.add(new ChatMessage(MessageRole.USER, new SystemReminder(
                    "你正在独立 Fork 中执行任务。继承的历史只用于理解上下文；不要假装修改父会话历史，只返回最终结果。")
                    .wrappedContent()));
        }
        return List.copyOf(result);
    }

    public List<SystemReminder> reminders(AgentDefinition definition) {
        return List.of();
    }

    public ChatMessage taskMessage(String task) {
        return new ChatMessage(MessageRole.USER, task);
    }
}
