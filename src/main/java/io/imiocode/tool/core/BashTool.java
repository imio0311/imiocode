package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.workspace.WorkspacePolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** 在固定工作区中运行平台 Shell 命令，并提供超时、取消和输出限制。 */
public final class BashTool extends BaseTool implements AutoCloseable {
    private final WorkspacePolicy policy;
    private final AtomicReference<Process> activeProcess = new AtomicReference<>();
    private volatile boolean closed;

    public BashTool(WorkspacePolicy policy, ToolLimits limits, SecretRedactor redactor) {
        super(createDefinition(), limits, redactor);
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) throws Exception {
        rejectUnknownFields(arguments, "command");
        String command = requireText(arguments, "command");
        if (closed) {
            return ToolResult.failure("命令工具已关闭");
        }
        ProcessBuilder builder = new ProcessBuilder(shellCommand(command));
        builder.directory(policy.workspace().toFile());
        redactor.removeSensitiveEnvironment(builder.environment());

        Process process = builder.start();
        if (!activeProcess.compareAndSet(null, process)) {
            terminate(process);
            return ToolResult.failure("已有命令正在执行");
        }
        try (ExecutorService drains = Executors.newVirtualThreadPerTaskExecutor()) {
            BoundedCapture stdout = new BoundedCapture(limits.maxCommandStdoutBytes());
            BoundedCapture stderr = new BoundedCapture(limits.maxCommandStderrBytes());
            Future<?> stdoutTask = drains.submit(() -> stdout.drain(process.getInputStream()));
            Future<?> stderrTask = drains.submit(() -> stderr.drain(process.getErrorStream()));
            boolean completed = process.waitFor(limits.commandTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                terminate(process);
            }
            awaitDrain(stdoutTask);
            awaitDrain(stderrTask);
            boolean truncated = stdout.truncated() || stderr.truncated();
            String output = stdout.text() + (stdout.truncated() ? BaseTool.TRUNCATION_MARKER : "");
            String error = stderr.text() + (stderr.truncated() ? BaseTool.TRUNCATION_MARKER : "");
            if (!completed) {
                return ToolResult.timeout(output, "命令执行超时" + suffix(error), truncated);
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return ToolResult.failure(output, error.isBlank() ? "命令退出码为 " + exitCode : error,
                        truncated, exitCode);
            }
            return new ToolResult(true, output, error, truncated, java.time.Duration.ZERO, exitCode);
        } finally {
            activeProcess.compareAndSet(process, null);
        }
    }

    @Override
    public void cancel() {
        Process process = activeProcess.get();
        if (process != null) {
            terminate(process);
        }
    }

    @Override
    public void close() {
        closed = true;
        cancel();
    }

    private List<String> shellCommand(String command) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return List.of(
                    "powershell.exe", "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", command);
        }
        return List.of("/bin/bash", "-lc", command);
    }

    private void terminate(Process process) {
        List<ProcessHandle> descendants = process.descendants()
                .sorted(Comparator.comparingLong(ProcessHandle::pid).reversed())
                .toList();
        descendants.forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            if (!process.waitFor(limits.processTerminationGrace().toMillis(), TimeUnit.MILLISECONDS)) {
                descendants.forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(limits.processTerminationGrace().toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            descendants.forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }

    private static void awaitDrain(Future<?> task) throws InterruptedException {
        try {
            task.get();
        } catch (ExecutionException exception) {
            throw new IllegalStateException("无法读取命令输出");
        }
    }

    private static String suffix(String error) {
        return error.isBlank() ? "" : "；stderr: " + error;
    }

    private static ToolDefinition createDefinition() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("command")
                .put("type", "string")
                .put("description", "交给平台 Shell 执行的命令");
        schema.putArray("required").add("command");
        schema.put("additionalProperties", false);
        return new ToolDefinition("bash", "在工作区中执行 Shell 命令", schema, ToolRisk.HIGH);
    }

    private static final class BoundedCapture {
        private final long limit;
        private final ByteArrayOutputStream kept = new ByteArrayOutputStream();
        private boolean truncated;

        private BoundedCapture(long limit) {
            this.limit = limit;
        }

        private void drain(InputStream input) {
            try (input) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    int remaining = (int) Math.max(0, Math.min(Integer.MAX_VALUE, limit - kept.size()));
                    int accepted = Math.min(count, remaining);
                    kept.write(buffer, 0, accepted);
                    truncated |= accepted < count;
                }
            } catch (IOException exception) {
                throw new IllegalStateException("读取命令输出失败");
            }
        }

        private boolean truncated() {
            return truncated;
        }

        private String text() {
            return kept.toString(StandardCharsets.UTF_8);
        }
    }
}
