package io.imiocode.conversation;

import io.imiocode.tool.ToolSelection;

import java.util.List;
import java.util.Optional;

/** Agent 每次迭代重新读取的会话策略，支持工具执行后立即收窄下一次模型请求。 */
public interface ConversationRuntimePolicy {
    Optional<ToolSelection> selection();
    List<SystemReminder> reminders();

    static ConversationRuntimePolicy inactive() {
        return new ConversationRuntimePolicy() {
            @Override public Optional<ToolSelection> selection() { return Optional.empty(); }
            @Override public List<SystemReminder> reminders() { return List.of(); }
        };
    }
}
