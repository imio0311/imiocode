package io.imiocode.command;

import java.util.Objects;

/** 与具体终端实现无关的通用确认内容。 */
public record ConfirmationPrompt(String title, String detail, String risk) {
    public ConfirmationPrompt {
        title = requireText(title, "确认标题");
        detail = requireText(detail, "确认目标");
        risk = Objects.requireNonNullElse(risk, "").trim();
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + "不能为空");
        return value.trim();
    }
}
