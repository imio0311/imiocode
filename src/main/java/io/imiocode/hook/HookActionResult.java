package io.imiocode.hook;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public record HookActionResult(HookExecutionStatus status, String output,
                               Optional<String> safeError, Duration elapsed) {
    public HookActionResult {
        status = Objects.requireNonNull(status, "status");
        output = Objects.requireNonNullElse(output, "");
        safeError = Objects.requireNonNullElse(safeError, Optional.empty());
        elapsed = Objects.requireNonNullElse(elapsed, Duration.ZERO);
    }
    public boolean successful() { return status == HookExecutionStatus.SUCCEEDED; }
    public static HookActionResult success(String output, Duration elapsed) {
        return new HookActionResult(HookExecutionStatus.SUCCEEDED, output, Optional.empty(), elapsed);
    }
    public static HookActionResult failure(String error, Duration elapsed) {
        return new HookActionResult(HookExecutionStatus.FAILED, "", Optional.ofNullable(error), elapsed);
    }
}
