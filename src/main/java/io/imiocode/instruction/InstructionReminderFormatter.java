package io.imiocode.instruction;

import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 按优先级把已展开项目指令格式化为会话级 System Reminder。 */
public final class InstructionReminderFormatter {
    public List<SystemReminder> format(InstructionSnapshot snapshot, Path workspace, Path userHome) {
        List<InstructionSource> ordered = snapshot.sources().stream()
                .sorted(Comparator.comparingInt(InstructionSource::priority)).toList();
        List<SystemReminder> reminders = new ArrayList<>(ordered.size());
        for (InstructionSource source : ordered) {
            Path base = source.scope() == InstructionScope.USER ? userHome.toAbsolutePath().normalize()
                    : workspace.toAbsolutePath().normalize();
            String display;
            try { display = base.relativize(source.path()).toString(); }
            catch (IllegalArgumentException exception) { display = source.path().getFileName().toString(); }
            String content = "项目指令来源：" + display.replace('\\', '/') + "\n"
                    + "作用域：" + source.scope().name().toLowerCase(java.util.Locale.ROOT) + "\n"
                    + "优先级：" + source.priority() + "（数字越大越优先；冲突时以后出现的规则为准）\n\n"
                    + source.expandedContent();
            reminders.add(new SystemReminder(ReminderScope.SESSION, content));
        }
        return List.copyOf(reminders);
    }
}
