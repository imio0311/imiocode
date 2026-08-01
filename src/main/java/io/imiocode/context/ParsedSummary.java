package io.imiocode.context;

public record ParsedSummary(String priorHistory, String activeTask) {
    public ParsedSummary {
        priorHistory = requireText(priorHistory, "priorHistory");
        activeTask = requireText(activeTask, "activeTask");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 不能为空");
        return value.trim();
    }
}
