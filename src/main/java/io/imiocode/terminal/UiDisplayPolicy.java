package io.imiocode.terminal;

import io.imiocode.config.UiVerbosity;
import io.imiocode.context.ContextEvent;
import io.imiocode.mcp.manager.McpEventType;
import io.imiocode.tool.ToolExecutionState;

import java.util.Objects;

/** 根据 UI 详细度决定哪些过程事件写入终端滚动区。 */
public final class UiDisplayPolicy {
    private final UiVerbosity verbosity;

    public UiDisplayPolicy(UiVerbosity verbosity) {
        this.verbosity = Objects.requireNonNull(verbosity, "verbosity");
    }

    public boolean showStateTransitions() {
        return verbose();
    }

    public boolean showThinking() {
        return verbose();
    }

    public boolean showUsage() {
        return verbose();
    }

    public boolean showToolEvent(ToolExecutionState state) {
        Objects.requireNonNull(state, "state");
        return verbose()
                || state == ToolExecutionState.SUCCEEDED
                || state == ToolExecutionState.FAILED;
    }

    public boolean showMcpEvent(McpEventType type) {
        Objects.requireNonNull(type, "type");
        return verbose()
                || type == McpEventType.DENIED
                || type == McpEventType.SERVER_FAILED;
    }

    public boolean showContextEvent(ContextEvent event) {
        Objects.requireNonNull(event, "event");
        return verbose() || !(event instanceof ContextEvent.Started);
    }

    private boolean verbose() {
        return verbosity == UiVerbosity.VERBOSE;
    }
}
