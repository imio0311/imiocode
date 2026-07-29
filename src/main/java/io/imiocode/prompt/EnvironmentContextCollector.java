package io.imiocode.prompt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** 从本地进程采集非敏感环境摘要。 */
public final class EnvironmentContextCollector implements EnvironmentContextProvider {
    private static final Duration OUTPUT_DRAIN_TIMEOUT = Duration.ofSeconds(1);

    private final Path workspace;
    private final Clock clock;
    private final Duration gitTimeout;

    public EnvironmentContextCollector(Path workspace, Clock clock, Duration gitTimeout) {
        this.workspace = Objects.requireNonNull(workspace, "工作区不能为空")
                .toAbsolutePath()
                .normalize();
        this.clock = Objects.requireNonNull(clock, "Clock 不能为空");
        this.gitTimeout = Objects.requireNonNull(gitTimeout, "Git 超时不能为空");
        if (gitTimeout.isZero() || gitTimeout.isNegative()) {
            throw new IllegalArgumentException("Git 超时必须为正数");
        }
    }

    @Override
    public EnvironmentContext capture() {
        return new EnvironmentContext(
                workspace,
                operatingSystem(),
                shellName(),
                ZonedDateTime.now(clock),
                inspectGit());
    }

    private static String operatingSystem() {
        String name = System.getProperty("os.name", "unknown").trim();
        String arch = System.getProperty("os.arch", "unknown").trim();
        return (name + " " + arch).trim();
    }

    private static String shellName() {
        try {
            Optional<String> parentCommand = ProcessHandle.current()
                    .parent()
                    .flatMap(handle -> handle.info().command());
            if (parentCommand.isPresent()) {
                Path command = Path.of(parentCommand.get());
                Path fileName = command.getFileName();
                if (fileName != null && !fileName.toString().isBlank()) {
                    return fileName.toString();
                }
            }
        } catch (RuntimeException ignored) {
            // 父进程信息不是完成任务的必要条件。
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("windows")) {
            return "powershell";
        }
        return "sh";
    }

    private GitContext inspectGit() {
        Process process;
        try {
            process = new ProcessBuilder(
                    "git",
                    "-C",
                    workspace.toString(),
                    "status",
                    "--porcelain=v1",
                    "--branch")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException | RuntimeException exception) {
            return GitContext.unavailable();
        }

        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readOutput(process));
        try {
            if (!process.waitFor(gitTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return GitContext.unavailable();
            }
            String text = output.get(
                    OUTPUT_DRAIN_TIMEOUT.toMillis(),
                    TimeUnit.MILLISECONDS);
            if (process.exitValue() != 0) {
                String normalized = text.toLowerCase(Locale.ROOT);
                GitWorkingTreeState state = normalized.contains("not a git repository")
                        ? GitWorkingTreeState.NOT_REPOSITORY
                        : GitWorkingTreeState.UNAVAILABLE;
                return new GitContext(Optional.empty(), state);
            }
            return parseGitStatus(text);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return GitContext.unavailable();
        } catch (Exception exception) {
            process.destroyForcibly();
            return GitContext.unavailable();
        }
    }

    private static String readOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private static GitContext parseGitStatus(String output) {
        List<String> lines = output.lines().toList();
        Optional<String> branch = lines.stream()
                .filter(line -> line.startsWith("## "))
                .findFirst()
                .flatMap(EnvironmentContextCollector::parseBranch);
        boolean dirty = lines.stream()
                .anyMatch(line -> !line.isBlank() && !line.startsWith("## "));
        return new GitContext(
                branch,
                dirty ? GitWorkingTreeState.DIRTY : GitWorkingTreeState.CLEAN);
    }

    private static Optional<String> parseBranch(String headerLine) {
        String value = headerLine.substring(3).trim();
        if (value.startsWith("No commits yet on ")) {
            value = value.substring("No commits yet on ".length());
        } else if (value.startsWith("Initial commit on ")) {
            value = value.substring("Initial commit on ".length());
        } else {
            int tracking = value.indexOf("...");
            if (tracking >= 0) {
                value = value.substring(0, tracking);
            }
            int detail = value.indexOf(' ');
            if (detail >= 0) {
                value = value.substring(0, detail);
            }
        }
        String sanitized = value.replaceAll("\\p{Cntrl}", "").trim();
        if (sanitized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(sanitized.substring(0, Math.min(sanitized.length(), 200)));
    }
}
