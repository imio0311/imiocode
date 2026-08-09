package io.imiocode.hook.action;

import java.time.Duration;
import java.util.Objects;

public record CommandAction(String command, Duration timeout) implements Action {
    public CommandAction {
        if (command == null || command.isBlank()) throw new IllegalArgumentException("command 不能为空");
        command = command.trim();
        timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout 必须大于 0");
    }
    @Override public HookActionType type() { return HookActionType.COMMAND; }
}
