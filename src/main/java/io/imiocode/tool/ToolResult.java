package io.imiocode.tool;

import java.time.Duration;
import java.util.Objects;

/** 工具执行完成后返回的统一、安全结果。 */
public record ToolResult(
        boolean success,
        String output,
        String error,
        boolean truncated,
        Duration duration,
        Integer exitCode) {
    public ToolResult {
        output = Objects.requireNonNullElse(output, "");
        error = Objects.requireNonNullElse(error, "");
        duration = Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration 不能为负数");
        }
    }

    public static ToolResult success(String output) {
        return success(output, false, null);
    }

    public static ToolResult success(String output, boolean truncated, Integer exitCode) {
        return new ToolResult(true, output, "", truncated, Duration.ZERO, exitCode);
    }

    public static ToolResult failure(String error) {
        return failure("", error, false, null);
    }

    public static ToolResult failure(
            String output,
            String error,
            boolean truncated,
            Integer exitCode) {
        return new ToolResult(false, output, error, truncated, Duration.ZERO, exitCode);
    }

    public static ToolResult timeout(String output, String error, boolean truncated) {
        return failure(output, error == null || error.isBlank() ? "命令执行超时" : error, truncated, null);
    }

    public static ToolResult interrupted(String output, String error, boolean truncated) {
        return failure(output, error == null || error.isBlank() ? "工具执行已中断" : error, truncated, null);
    }

    public ToolResult withDuration(Duration measuredDuration) {
        return new ToolResult(success, output, error, truncated, measuredDuration, exitCode);
    }
}
