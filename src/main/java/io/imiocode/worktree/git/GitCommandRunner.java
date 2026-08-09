package io.imiocode.worktree.git;

import io.imiocode.worktree.WorktreeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.FutureTask;

/** 使用参数数组执行 Git；不经过 Shell，输出有界。 */
public final class GitCommandRunner {
    private static final int MAX_OUTPUT_BYTES = 1024 * 1024;
    private final Duration timeout;

    public GitCommandRunner(Duration timeout) {
        this.timeout = Objects.requireNonNull(timeout);
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout 必须为正数");
    }

    public GitCommandResult run(Path cwd, List<String> arguments) {
        Objects.requireNonNull(cwd); Objects.requireNonNull(arguments);
        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add("git"); command.addAll(arguments);
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(cwd.toAbsolutePath().normalize().toFile())
                    .redirectErrorStream(true);
            process = builder.start();
            Process running = process;
            FutureTask<String> outputTask = new FutureTask<>(() -> readBounded(running.getInputStream()));
            Thread reader = Thread.ofVirtual().start(outputTask);
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
                throw new WorktreeException("Git 操作超时");
            }
            reader.join();
            return new GitCommandResult(process.exitValue(), outputTask.get());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (process != null) process.destroyForcibly();
            throw new WorktreeException("Git 操作已取消", exception);
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new WorktreeException("无法读取 Git 输出", exception);
        } catch (IOException exception) {
            throw new WorktreeException("无法启动 Git", exception);
        }
    }

    public String checked(Path cwd, List<String> arguments, String safeFailure) {
        GitCommandResult result = run(cwd, arguments);
        if (!result.success()) throw new WorktreeException(safeFailure);
        return result.output().strip();
    }

    private static String readBounded(InputStream input) {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                int remaining = MAX_OUTPUT_BYTES - output.size();
                if (remaining > 0) output.write(buffer, 0, Math.min(remaining, count));
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

}
