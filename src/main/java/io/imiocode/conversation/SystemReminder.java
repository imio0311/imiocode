package io.imiocode.conversation;

public record SystemReminder(String content) {
    public SystemReminder {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("system-reminder 不能为空");
        }
        content = content.trim();
    }
}
