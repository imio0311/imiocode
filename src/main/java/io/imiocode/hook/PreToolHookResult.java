package io.imiocode.hook;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PreToolHookResult(List<HookExecutionRecord> executions,
                                Optional<ToolRejectedError> rejection) {
    public static final PreToolHookResult ALLOW = new PreToolHookResult(List.of(), Optional.empty());
    public PreToolHookResult {
        executions = List.copyOf(Objects.requireNonNullElse(executions, List.of()));
        rejection = Objects.requireNonNullElse(rejection, Optional.empty());
    }
    public boolean allowed() { return rejection.isEmpty(); }
}
