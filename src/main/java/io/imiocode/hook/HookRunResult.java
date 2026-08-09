package io.imiocode.hook;

import io.imiocode.conversation.SystemReminder;
import java.util.List;
import java.util.Objects;

public record HookRunResult(List<HookExecutionRecord> executions, List<SystemReminder> reminders) {
    public static final HookRunResult EMPTY = new HookRunResult(List.of(), List.of());
    public HookRunResult {
        executions = List.copyOf(Objects.requireNonNullElse(executions, List.of()));
        reminders = List.copyOf(Objects.requireNonNullElse(reminders, List.of()));
    }
}
