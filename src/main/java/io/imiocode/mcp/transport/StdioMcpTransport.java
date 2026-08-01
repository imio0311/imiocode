package io.imiocode.mcp.transport;

import io.imiocode.mcp.config.ResolvedMcpServerConfig;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.jsonrpc.JsonRpcMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 使用受隔离子进程 stdin/stdout 的 MCP Transport。 */
public final class StdioMcpTransport implements McpTransport {
    static final int MAX_MESSAGE_BYTES = 4 * 1024 * 1024;
    static final int MAX_STDERR_CHARS = 64 * 1024;
    private static final Duration TERMINATION_GRACE = Duration.ofSeconds(2);

    private final ResolvedMcpServerConfig config;
    private final JsonRpcCodec codec;
    private final Object writeLock = new Object();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean failed = new AtomicBoolean();
    private final StringBuilder stderrTail = new StringBuilder();

    private volatile Process process;
    private volatile Writer stdin;
    private volatile Thread stdoutThread;
    private volatile Thread stderrThread;
    private volatile Consumer<JsonRpcMessage> inboundHandler;
    private volatile Consumer<Throwable> failureHandler;

    public StdioMcpTransport(ResolvedMcpServerConfig config, JsonRpcCodec codec) {
        this.config = Objects.requireNonNull(config, "config");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public CompletableFuture<Void> start(
            Consumer<JsonRpcMessage> inboundHandler,
            Consumer<Throwable> failureHandler) {
        if (!started.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new McpTransportException("stdio Transport 已启动"));
        }
        this.inboundHandler = Objects.requireNonNull(inboundHandler, "inboundHandler");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
        try {
            List<String> command = new ArrayList<>();
            command.add(config.command());
            command.addAll(config.args());
            ProcessBuilder builder = new ProcessBuilder(command);
            Map<String, String> childEnvironment = builder.environment();
            childEnvironment.clear();
            findPath().ifPresent(path -> childEnvironment.put("PATH", path));
            childEnvironment.putAll(config.env());
            Path sourceParent = config.source().getParent();
            if (sourceParent != null) {
                Path workingDirectory = sourceParent.getFileName() != null
                        && ".imiocode".equals(sourceParent.getFileName().toString())
                        ? sourceParent.getParent()
                        : sourceParent;
                if (workingDirectory != null) {
                    builder.directory(workingDirectory.toFile());
                }
            }
            process = builder.start();
            stdin = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
            stdoutThread = Thread.ofVirtual()
                    .name("mcp-stdio-" + config.name() + "-stdout")
                    .start(() -> readStdout(process.getInputStream()));
            stderrThread = Thread.ofVirtual()
                    .name("mcp-stdio-" + config.name() + "-stderr")
                    .start(() -> readStderr(process.getErrorStream()));
            return CompletableFuture.completedFuture(null);
        } catch (IOException | RuntimeException exception) {
            closed.set(true);
            return CompletableFuture.failedFuture(
                    new McpTransportException("无法启动 MCP stdio Server: " + config.name(), exception));
        }
    }

    @Override
    public CompletableFuture<Void> send(JsonRpcMessage message) {
        Objects.requireNonNull(message, "message");
        if (!isOpen()) {
            return CompletableFuture.failedFuture(new McpTransportException("MCP stdio 连接未打开"));
        }
        try {
            String encoded = codec.encode(message);
            if (encoded.getBytes(StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) {
                return CompletableFuture.failedFuture(new McpTransportException("MCP 消息超过大小限制"));
            }
            synchronized (writeLock) {
                stdin.write(encoded);
                stdin.write('\n');
                stdin.flush();
            }
            return CompletableFuture.completedFuture(null);
        } catch (IOException | RuntimeException exception) {
            McpTransportException failure = new McpTransportException("写入 MCP stdio 失败", exception);
            signalFailure(failure);
            return CompletableFuture.failedFuture(failure);
        }
    }

    @Override
    public boolean isOpen() {
        Process current = process;
        return started.get() && !closed.get() && current != null && current.isAlive();
    }

    public String stderrTail() {
        synchronized (stderrTail) {
            return stderrTail.toString();
        }
    }

    private void readStdout(InputStream stream) {
        try (stream) {
            while (!closed.get()) {
                byte[] line = readBoundedLine(stream);
                if (line == null) {
                    if (!closed.get()) {
                        signalFailure(new McpTransportException("MCP stdio 管道已关闭"));
                    }
                    return;
                }
                if (line.length == 0) {
                    continue;
                }
                JsonRpcMessage message = codec.decode(new String(line, StandardCharsets.UTF_8));
                inboundHandler.accept(message);
            }
        } catch (IOException | RuntimeException exception) {
            if (!closed.get()) {
                signalFailure(new McpTransportException("读取 MCP stdio 失败", exception));
            }
        }
    }

    private void readStderr(InputStream stream) {
        byte[] buffer = new byte[4_096];
        try (stream) {
            int count;
            while (!closed.get() && (count = stream.read(buffer)) >= 0) {
                if (count == 0) {
                    continue;
                }
                String chunk = new String(buffer, 0, count, StandardCharsets.UTF_8);
                synchronized (stderrTail) {
                    stderrTail.append(chunk);
                    if (stderrTail.length() > MAX_STDERR_CHARS) {
                        stderrTail.delete(0, stderrTail.length() - MAX_STDERR_CHARS);
                    }
                }
            }
        } catch (IOException ignored) {
            // stderr 仅用于诊断，关闭时出现读取错误不覆盖主错误。
        }
    }

    private static byte[] readBoundedLine(InputStream stream) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        while (true) {
            int next = stream.read();
            if (next < 0) {
                return line.size() == 0 ? null : line.toByteArray();
            }
            if (next == '\n') {
                byte[] bytes = line.toByteArray();
                if (bytes.length > 0 && bytes[bytes.length - 1] == '\r') {
                    return java.util.Arrays.copyOf(bytes, bytes.length - 1);
                }
                return bytes;
            }
            if (line.size() >= MAX_MESSAGE_BYTES) {
                throw new McpTransportException("MCP stdio 消息超过大小限制");
            }
            line.write(next);
        }
    }

    private void signalFailure(Throwable failure) {
        if (failed.compareAndSet(false, true)) {
            Consumer<Throwable> handler = failureHandler;
            if (handler != null) {
                handler.accept(failure);
            }
        }
    }

    private static java.util.Optional<String> findPath() {
        return System.getenv().entrySet().stream()
                .filter(entry -> "PATH".equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Writer currentStdin = stdin;
        if (currentStdin != null) {
            try {
                currentStdin.close();
            } catch (IOException ignored) {
                // 继续回收子进程。
            }
        }
        Process currentProcess = process;
        if (currentProcess != null && currentProcess.isAlive()) {
            try {
                if (!currentProcess.waitFor(TERMINATION_GRACE.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    currentProcess.destroy();
                    if (!currentProcess.waitFor(
                            TERMINATION_GRACE.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                        currentProcess.destroyForcibly();
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                currentProcess.destroyForcibly();
            }
        }
        if (stdoutThread != null) {
            stdoutThread.interrupt();
        }
        if (stderrThread != null) {
            stderrThread.interrupt();
        }
    }
}
