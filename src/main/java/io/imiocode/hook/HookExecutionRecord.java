package io.imiocode.hook;

import java.util.Objects;

public record HookExecutionRecord(String hookId, HookEvent event, HookActionResult result) {
    public HookExecutionRecord {
        hookId = Objects.requireNonNull(hookId, "hookId");
        event = Objects.requireNonNull(event, "event");
        result = Objects.requireNonNull(result, "result");
    }
}
