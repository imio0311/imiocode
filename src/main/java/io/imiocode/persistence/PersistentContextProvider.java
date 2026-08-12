package io.imiocode.persistence;

import io.imiocode.conversation.SystemReminder;
import java.util.List;

/** 提供可在每轮对话前重新加载的项目指令与记忆提醒。 */
public interface PersistentContextProvider {
    List<SystemReminder> currentReminders();
    void reloadInstructions();
    void reloadMemories();
}
