package io.imiocode.terminal;

public record ConfirmationPrompt(String title, String detail, String risk) {
    public ConfirmationPrompt {
        if (title == null || title.isBlank() || detail == null || detail.isBlank()) {
            throw new IllegalArgumentException("确认提示不能为空");
        }
        risk = risk == null ? "" : risk;
    }
}
