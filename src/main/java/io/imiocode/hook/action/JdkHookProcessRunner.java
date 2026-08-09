package io.imiocode.hook.action;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** 有界输出、可取消并会终止子进程树的 JDK 进程执行器。 */
public final class JdkHookProcessRunner implements HookProcessRunner {
    static final int OUTPUT_LIMIT = 64 * 1024;
    private final Set<Process> active = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    @Override public ProcessResult run(String command, Path workspace,
                                       Map<String, String> environment, Duration timeout) {
        long start = System.nanoTime();
        if (closed.get()) return failed("Hook 进程执行器已关闭", start);
        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(shell(command));
            builder.directory(workspace.toFile());
            builder.environment().clear();
            builder.environment().putAll(environment);
            process = builder.start();
        } catch (IOException exception) {
            return failed("无法启动 Hook 命令", start);
        }
        active.add(process);
        try (var drains = Executors.newVirtualThreadPerTaskExecutor()) {
            Capture stdout = new Capture(); Capture stderr = new Capture();
            Future<?> out = drains.submit(() -> stdout.read(process.getInputStream()));
            Future<?> err = drains.submit(() -> stderr.read(process.getErrorStream()));
            boolean completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) terminate(process);
            await(out); await(err);
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            if (!completed) return new ProcessResult(true, true, -1, stdout.text(), stderr.text(),
                    stdout.truncated || stderr.truncated, elapsed);
            return new ProcessResult(true, false, process.exitValue(), stdout.text(), stderr.text(),
                    stdout.truncated || stderr.truncated, elapsed);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); terminate(process);
            return new ProcessResult(true, true, -1, "", "Hook 命令已中断", false,
                    Duration.ofNanos(System.nanoTime() - start));
        } finally {
            active.remove(process);
        }
    }

    private static List<String> shell(String command) {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        return os.contains("win") ? List.of("cmd.exe", "/d", "/s", "/c", command)
                : List.of("/bin/sh", "-c", command);
    }

    private static void await(Future<?> task) {
        try { task.get(); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        catch (ExecutionException ignored) { }
    }

    private static void terminate(Process process) {
        List<ProcessHandle> children = process.descendants()
                .sorted(Comparator.comparingLong(ProcessHandle::pid).reversed()).toList();
        children.forEach(ProcessHandle::destroy); process.destroy();
        try {
            if (!process.waitFor(300, TimeUnit.MILLISECONDS)) {
                children.forEach(ProcessHandle::destroyForcibly); process.destroyForcibly();
                process.waitFor(300, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            children.forEach(ProcessHandle::destroyForcibly); process.destroyForcibly();
        }
    }

    private static ProcessResult failed(String message, long start) {
        return new ProcessResult(false, false, -1, "", message, false,
                Duration.ofNanos(System.nanoTime() - start));
    }

    @Override public void close() {
        if (closed.compareAndSet(false, true)) {
            new ArrayList<>(active).forEach(JdkHookProcessRunner::terminate);
            active.clear();
        }
    }

    private static final class Capture {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private boolean truncated;
        private void read(InputStream input) {
            try (input) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) >= 0) {
                    int accepted = Math.min(count, Math.max(0, OUTPUT_LIMIT - bytes.size()));
                    bytes.write(buffer, 0, accepted); truncated |= accepted < count;
                }
            } catch (IOException ignored) { }
        }
        private String text() { return bytes.toString(StandardCharsets.UTF_8); }
    }
}
