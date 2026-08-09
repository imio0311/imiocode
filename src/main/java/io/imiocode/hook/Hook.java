package io.imiocode.hook;

import io.imiocode.hook.action.Action;
import io.imiocode.hook.condition.ConditionGroup;

import java.util.Objects;
import java.util.Optional;

/** 已校验、可直接调度的 Hook。 */
public record Hook(String id, HookEvent event, Optional<ConditionGroup> condition,
                   Action action, boolean once, boolean async, boolean reject,
                   Optional<String> rejectMessage, HookFailurePolicy onError) {
    public Hook {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Hook id 不能为空");
        id = id.trim();
        event = Objects.requireNonNull(event, "event");
        condition = Objects.requireNonNullElse(condition, Optional.empty());
        action = Objects.requireNonNull(action, "action");
        rejectMessage = (rejectMessage == null ? Optional.<String>empty() : rejectMessage)
                .map(String::trim).filter(value -> !value.isEmpty());
        onError = Objects.requireNonNullElse(onError, HookFailurePolicy.IGNORE);
    }
}
