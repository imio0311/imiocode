package io.imiocode.memory;

import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 将用户级和项目级长期记忆按固定优先关系注入会话提醒。 */
public final class MemoryReminderFormatter {
    public List<SystemReminder> format(List<MemoryDocument> documents) {
        List<SystemReminder> result = new ArrayList<>();
        for (MemoryScope scope : MemoryScope.values()) {
            MemoryDocument document = documents.stream().filter(item -> item.scope() == scope).findFirst()
                    .orElse(MemoryDocument.empty(scope));
            if (document.entries().isEmpty()) continue;
            StringBuilder content = new StringBuilder(scope == MemoryScope.USER
                    ? "用户长期偏好（与项目记忆冲突时项目记忆优先）：\n"
                    : "当前项目长期知识（冲突时优先于用户记忆）：\n");
            document.entries().stream().sorted(Comparator.comparing(MemoryEntry::id)).forEach(entry -> content
                    .append("- [").append(entry.id()).append("] [")
                    .append(entry.category().name().toLowerCase(Locale.ROOT)).append("] ")
                    .append(entry.content()).append('\n'));
            result.add(new SystemReminder(ReminderScope.SESSION, content.toString()));
        }
        return List.copyOf(result);
    }
}
