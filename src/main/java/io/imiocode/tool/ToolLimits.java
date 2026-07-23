package io.imiocode.tool;

import java.time.Duration;
import java.util.Objects;

/** 第三章固定且不可在运行时修改的资源限制。 */
public record ToolLimits(
        long maxReadBytes,
        int maxReadLines,
        long maxWriteBytes,
        int maxScannedPaths,
        int maxGlobResults,
        int maxGrepResults,
        int maxGrepLineChars,
        long maxResultBytes,
        long maxCommandStdoutBytes,
        long maxCommandStderrBytes,
        Duration commandTimeout,
        Duration processTerminationGrace) {

    public ToolLimits {
        requirePositive(maxReadBytes, "maxReadBytes");
        requirePositive(maxReadLines, "maxReadLines");
        requirePositive(maxWriteBytes, "maxWriteBytes");
        requirePositive(maxScannedPaths, "maxScannedPaths");
        requirePositive(maxGlobResults, "maxGlobResults");
        requirePositive(maxGrepResults, "maxGrepResults");
        requirePositive(maxGrepLineChars, "maxGrepLineChars");
        requirePositive(maxResultBytes, "maxResultBytes");
        requirePositive(maxCommandStdoutBytes, "maxCommandStdoutBytes");
        requirePositive(maxCommandStderrBytes, "maxCommandStderrBytes");
        commandTimeout = requirePositive(commandTimeout, "commandTimeout");
        processTerminationGrace = requirePositive(processTerminationGrace, "processTerminationGrace");
    }

    public static ToolLimits defaults() {
        return new ToolLimits(
                256L * 1024,
                2_000,
                1024L * 1024,
                20_000,
                1_000,
                200,
                2_000,
                512L * 1024,
                128L * 1024,
                128L * 1024,
                Duration.ofSeconds(30),
                Duration.ofSeconds(2));
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " 必须为正数");
        }
    }

    private static Duration requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " 必须为正数");
        }
        return value;
    }
}
