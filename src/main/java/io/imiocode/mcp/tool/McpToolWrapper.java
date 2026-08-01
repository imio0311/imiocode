package io.imiocode.mcp.tool;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.mcp.client.McpCallHandle;
import io.imiocode.mcp.client.McpCallResult;
import io.imiocode.mcp.client.McpClient;
import io.imiocode.mcp.client.McpRemoteTool;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/** 将一个 MCP 远程工具包装成 ImioCode 的同步 Tool 接口。 */
public final class McpToolWrapper extends BaseTool {
    private final McpClient client;
    private final McpRemoteTool remoteTool;
    private final Duration callTimeout;
    private final McpToolResultMapper resultMapper;
    private final AtomicReference<McpCallHandle> currentCall = new AtomicReference<>();

    public McpToolWrapper(
            String serverName,
            McpClient client,
            McpRemoteTool remoteTool,
            Duration callTimeout,
            ToolLimits limits,
            SecretRedactor redactor) {
        this(serverName, client, remoteTool, callTimeout, limits, redactor,
                new McpToolResultMapper());
    }

    McpToolWrapper(
            String serverName,
            McpClient client,
            McpRemoteTool remoteTool,
            Duration callTimeout,
            ToolLimits limits,
            SecretRedactor redactor,
            McpToolResultMapper resultMapper) {
        super(new ToolDefinition(
                        McpToolName.create(serverName, remoteTool.name()),
                        remoteTool.description(),
                        remoteTool.inputSchema(),
                        ToolRisk.HIGH),
                limits,
                redactor);
        this.client = Objects.requireNonNull(client, "client");
        this.remoteTool = Objects.requireNonNull(remoteTool, "remoteTool");
        this.callTimeout = Objects.requireNonNull(callTimeout, "callTimeout");
        this.resultMapper = Objects.requireNonNull(resultMapper, "resultMapper");
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) throws Exception {
        McpCallHandle handle = client.callTool(remoteTool.name(), arguments, callTimeout);
        if (!currentCall.compareAndSet(null, handle)) {
            return ToolResult.failure("同一个 MCP 工具已有调用正在执行");
        }
        try {
            McpCallResult result = handle.result().get(callTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return resultMapper.map(result);
        } catch (TimeoutException exception) {
            client.cancelRequest(handle.requestId(), "timeout");
            return ToolResult.timeout("", "MCP 工具调用超时", false);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            String message = cause == null || cause.getMessage() == null || cause.getMessage().isBlank()
                    ? "MCP 工具调用失败"
                    : cause.getMessage();
            return ToolResult.failure(message);
        } finally {
            currentCall.compareAndSet(handle, null);
        }
    }

    @Override
    public void cancel() {
        McpCallHandle handle = currentCall.getAndSet(null);
        if (handle != null) {
            client.cancelRequest(handle.requestId(), "agent_cancelled");
        }
    }
}
