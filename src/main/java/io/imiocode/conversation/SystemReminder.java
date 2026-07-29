package io.imiocode.conversation;

import java.util.Locale;
import java.util.Objects;

public record SystemReminder(ReminderScope scope, String content) {
    public SystemReminder {
        scope = Objects.requireNonNull(scope, "system-reminder 作用域不能为空");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("system-reminder 不能为空");
        }
        content = content.trim();
        String normalized = content.toLowerCase(Locale.ROOT);
        if (normalized.contains("<system-reminder")
                || normalized.contains("</system-reminder>")) {
            throw new IllegalArgumentException("system-reminder 正文不能嵌套保留标签");
        }
    }

    public SystemReminder(String content) {
        this(ReminderScope.SESSION, content);
    }

    public String wrappedContent() {
        return "<system-reminder>\n" + content + "\n</system-reminder>";
    }
}
