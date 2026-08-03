package io.imiocode.persistence;

import io.imiocode.conversation.SystemReminder;
import java.util.List;

public interface PersistentContextProvider {
    List<SystemReminder> currentReminders();
    void reloadInstructions();
    void reloadMemories();
}
