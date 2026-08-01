package io.imiocode.mcp.manager;

import io.imiocode.mcp.client.DefaultMcpClient;
import io.imiocode.mcp.client.McpClient;
import io.imiocode.mcp.client.McpRemoteTool;
import io.imiocode.mcp.config.McpConfigError;
import io.imiocode.mcp.config.McpTransportType;
import io.imiocode.mcp.config.ResolvedMcpServerConfig;
import io.imiocode.mcp.tool.McpToolWrapper;
import io.imiocode.mcp.transport.McpTransportFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/** 管理全部 MCP Server 的审批、连接、工具注册与关闭。 */
public final class McpManager implements AutoCloseable {
    private final ClientFactory clientFactory;
    private final McpLaunchApprover approver;
    private final McpEventListener listener;
    private final ToolLimits limits;
    private final SecretRedactor redactor;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, McpClient> activeClients = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public McpManager(
            McpTransportFactory transportFactory,
            McpLaunchApprover approver,
            McpEventListener listener,
            ToolLimits limits,
            SecretRedactor redactor,
            String clientVersion) {
        this(config -> new DefaultMcpClient(
                        transportFactory.create(config),
                        "ImioCode",
                        clientVersion,
                        config.initializationTimeout(),
                        config.callTimeout()),
                approver, listener, limits, redactor);
    }

    McpManager(
            ClientFactory clientFactory,
            McpLaunchApprover approver,
            McpEventListener listener,
            ToolLimits limits,
            SecretRedactor redactor) {
        this.clientFactory = Objects.requireNonNull(clientFactory, "clientFactory");
        this.approver = Objects.requireNonNull(approver, "approver");
        this.listener = Objects.requireNonNullElse(listener, McpEventListener.NOOP);
        this.limits = Objects.requireNonNull(limits, "limits");
        this.redactor = Objects.requireNonNull(redactor, "redactor");
    }

    public McpStartupResult start(
            Collection<ResolvedMcpServerConfig> configs,
            ToolRegistry registry) {
        Objects.requireNonNull(configs, "configs");
        Objects.requireNonNull(registry, "registry");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("MCP Manager 已启动");
        }
        List<ResolvedMcpServerConfig> approved = new ArrayList<>();
        configs.stream().sorted(Comparator.comparing(ResolvedMcpServerConfig::name))
                .forEach(config -> {
                    if (config.transport() != McpTransportType.STDIO) {
                        approved.add(config);
                        return;
                    }
                    emit(McpEventType.WAITING_FOR_APPROVAL, config.name(), "", "等待启动确认");
                    boolean accepted = approver.approve(new McpLaunchRequest(
                            config.name(), config.command(), config.args()));
                    emit(accepted ? McpEventType.APPROVED : McpEventType.DENIED,
                            config.name(), "", accepted ? "已批准" : "已拒绝");
                    if (accepted) {
                        approved.add(config);
                    }
                });

        Map<String, Future<ConnectionOutcome>> futures = new LinkedHashMap<>();
        for (ResolvedMcpServerConfig config : approved) {
            emit(McpEventType.CONNECTING, config.name(), "", "正在连接");
            futures.put(config.name(), executor.submit(() -> connect(config)));
        }
        List<McpConfigError> errors = new ArrayList<>();
        int registered = 0;
        for (ResolvedMcpServerConfig config : approved) {
            ConnectionOutcome outcome;
            try {
                outcome = futures.get(config.name()).get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                outcome = ConnectionOutcome.failure(config, "MCP 连接被中断");
            } catch (ExecutionException exception) {
                outcome = ConnectionOutcome.failure(config, "MCP Server 连接失败");
            }
            if (outcome.error() != null) {
                errors.add(new McpConfigError(
                        config.source(), config.name(), "connection_failed", outcome.error()));
                emit(McpEventType.SERVER_FAILED, config.name(), "", outcome.error());
                continue;
            }
            activeClients.put(config.name(), outcome.client());
            emit(McpEventType.CONNECTED, config.name(), "",
                    "已连接，发现 " + outcome.tools().size() + " 个工具");
            for (McpRemoteTool remoteTool : outcome.tools().stream()
                    .sorted(Comparator.comparing(McpRemoteTool::name))
                    .toList()) {
                try {
                    McpToolWrapper wrapper = new McpToolWrapper(
                            config.name(),
                            outcome.client(),
                            remoteTool,
                            config.callTimeout(),
                            limits,
                            redactor);
                    registry.register(wrapper);
                    registered++;
                    emit(McpEventType.TOOL_DISCOVERED,
                            config.name(), wrapper.definition().name(), "工具已注册");
                } catch (IllegalArgumentException exception) {
                    errors.add(new McpConfigError(
                            config.source(),
                            config.name(),
                            "tool_registration_failed",
                            "MCP 工具名称无效或发生冲突"));
                }
            }
        }
        return new McpStartupResult(activeClients.size(), registered, errors);
    }

    private ConnectionOutcome connect(ResolvedMcpServerConfig config) {
        McpClient client = null;
        try {
            client = clientFactory.create(config);
            client.connect().get();
            List<McpRemoteTool> tools = client.listTools().get();
            return ConnectionOutcome.success(config, client, tools);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (client != null) {
                client.close();
            }
            return ConnectionOutcome.failure(config, "MCP 连接被中断");
        } catch (Exception exception) {
            if (client != null) {
                client.close();
            }
            return ConnectionOutcome.failure(config, safeFailure(exception));
        }
    }

    public Map<String, McpClient> activeClients() {
        return Map.copyOf(activeClients);
    }

    private String safeFailure(Exception exception) {
        Throwable current = exception;
        while ((current instanceof ExecutionException
                || current instanceof java.util.concurrent.CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return redactor.redact(message == null || message.isBlank()
                ? "MCP Server 连接失败"
                : message);
    }

    private void emit(McpEventType type, String server, String tool, String message) {
        listener.onMcpEvent(new McpEvent(type, server, tool, redactor.redact(message)));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        activeClients.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    try {
                        entry.getValue().close();
                    } finally {
                        emit(McpEventType.CLOSED, entry.getKey(), "", "连接已关闭");
                    }
                });
        activeClients.clear();
        executor.shutdownNow();
    }

    @FunctionalInterface
    interface ClientFactory {
        McpClient create(ResolvedMcpServerConfig config);
    }

    private record ConnectionOutcome(
            ResolvedMcpServerConfig config,
            McpClient client,
            List<McpRemoteTool> tools,
            String error) {
        static ConnectionOutcome success(
                ResolvedMcpServerConfig config,
                McpClient client,
                List<McpRemoteTool> tools) {
            return new ConnectionOutcome(config, client, List.copyOf(tools), null);
        }

        static ConnectionOutcome failure(ResolvedMcpServerConfig config, String error) {
            return new ConnectionOutcome(config, null, List.of(), error);
        }
    }
}
