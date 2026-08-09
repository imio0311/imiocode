package io.imiocode.hook.action;

public record PromptAction(String message) implements Action {
    public PromptAction {
        if (message == null || message.isBlank()) throw new IllegalArgumentException("message 不能为空");
        message = message.trim();
    }
    @Override public HookActionType type() { return HookActionType.PROMPT; }
}
