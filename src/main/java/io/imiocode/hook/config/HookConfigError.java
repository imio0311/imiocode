package io.imiocode.hook.config;

import java.util.Objects;

public record HookConfigError(int index, String hookId, String message) {
    public HookConfigError {
        hookId = Objects.requireNonNullElse(hookId, "");
        message = Objects.requireNonNullElse(message, "Hook 配置无效");
    }
    public String safeMessage() {
        String label = hookId.isBlank() ? "hooks[" + index + "]" : "hooks[" + index + "](" + hookId + ")";
        return label + ": " + message;
    }
}
