package io.imiocode.instruction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** 在有限时间内定位 Git 根目录，失败时安全回退到工作目录。 */
public final class GitProjectLocator {
    private final Duration timeout;

    public GitProjectLocator() { this(Duration.ofSeconds(2)); }

    public GitProjectLocator(Duration timeout) { this.timeout = Objects.requireNonNull(timeout, "timeout"); }

    public Path locate(Path workspace) {
        Path normalized = workspace.toAbsolutePath().normalize();
        Process process = null;
        try {
            process = new ProcessBuilder("git", "rev-parse", "--show-toplevel")
                    .directory(normalized.toFile()).redirectErrorStream(true).start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return normalized;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0 || output.isBlank()) return normalized;
            Path candidate = Path.of(output).toAbsolutePath().normalize();
            return normalized.startsWith(candidate) ? candidate : normalized;
        } catch (IOException | InterruptedException | RuntimeException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            return normalized;
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
    }
}
