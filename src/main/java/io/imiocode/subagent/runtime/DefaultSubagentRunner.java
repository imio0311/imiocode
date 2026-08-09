package io.imiocode.subagent.runtime;

import io.imiocode.agent.AgentEvent;
import io.imiocode.agent.AgentRequest;
import io.imiocode.agent.AgentResult;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.permission.PermissionReply;
import io.imiocode.subagent.context.SubagentContextBuilder;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.subagent.filter.SubagentToolFilter;
import io.imiocode.subagent.trace.TraceRegistry;
import io.imiocode.subagent.trace.TraceStatus;
import io.imiocode.subagent.trace.TraceTokenUsage;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolSelection;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** 独立运行一个子 Agent 到终态；权限确认在后台一律拒绝，避免隐藏任务阻塞。 */
public final class DefaultSubagentRunner implements SubagentRunner {
    private final ToolRegistry tools;
    private final SubagentToolFilter filter;
    private final SubagentContextBuilder contexts;
    private final SubagentAgentFactory agents;
    private final TraceRegistry traces;
    private final String parentTraceId;

    public DefaultSubagentRunner(ToolRegistry tools, SubagentToolFilter filter,
                                 SubagentContextBuilder contexts, SubagentAgentFactory agents,
                                 TraceRegistry traces) {
        this(tools, filter, contexts, agents, traces, null);
    }

    public DefaultSubagentRunner(ToolRegistry tools, SubagentToolFilter filter,
                                 SubagentContextBuilder contexts, SubagentAgentFactory agents,
                                 TraceRegistry traces, String parentTraceId) {
        this.tools=Objects.requireNonNull(tools); this.filter=Objects.requireNonNull(filter);
        this.contexts=Objects.requireNonNull(contexts); this.agents=Objects.requireNonNull(agents);
        this.traces=Objects.requireNonNull(traces);
        this.parentTraceId=parentTraceId;
    }

    @Override public SubagentRunResult run(AgentDefinition definition, String task,
            List<ChatMessage> parentHistory, boolean background, SubagentRunMode mode,
            CancellationRegistration cancellation) {
        ToolSelection selection = filter.select(definition, tools.enabledNames(), background);
        String traceId = null;
        AtomicReference<TraceTokenUsage> usage = new AtomicReference<>(TraceTokenUsage.zero());
        try (SubagentAgentHandle handle = agents.create(definition, selection)) {
            final String startedTraceId = traces.start(definition.name(), parentTraceId, handle.model(), background);
            traceId = startedTraceId;
            cancellation.register(handle.agent()::cancelActive);
            AgentResult result = handle.agent().run(new AgentRequest(
                    contexts.history(definition, parentHistory, mode), contexts.taskMessage(task),
                    contexts.reminders(definition), selection), event -> {
                if (event instanceof AgentEvent.ModelResponseCompleted completed) {
                    traces.addUsage(startedTraceId, completed.usage());
                    usage.updateAndGet(value -> value.plus(completed.usage()));
                } else if (event instanceof AgentEvent.PermissionRequested requested) {
                    // Fork 没有可安全复用的终端输入通道；ASK 必须 fail closed。
                    handle.agent().respondPermission(requested.prompt().requestId(), PermissionReply.DENY);
                }
            });
            String prefix = handle.warning().map(value -> "[警告] " + value + "\n").orElse("");
            if (result.completed()) {
                traces.finish(traceId, TraceStatus.COMPLETED, result.stopReason().name(), null);
                return new SubagentRunResult(true, prefix + result.finalResponse().orElseThrow().text(),
                        result.stopReason(), usage.get(), traceId);
            }
            TraceStatus status = switch (result.stopReason()) {
                case CANCELLED -> TraceStatus.CANCELLED;
                case TIMEOUT -> TraceStatus.TIMED_OUT;
                default -> TraceStatus.FAILED;
            };
            String message = result.error().map(e -> e.safeMessage()).orElse("子 Agent 停止: " + result.stopReason());
            traces.finish(traceId, status, result.stopReason().name(), message);
            return new SubagentRunResult(false, prefix + message, result.stopReason(), usage.get(), traceId);
        } catch (RuntimeException exception) {
            if (traceId == null) traceId = traces.start(definition.name(), parentTraceId,
                    definition.model().orElse("unknown"), background);
            traces.finish(traceId, TraceStatus.FAILED, "子 Agent 执行失败");
            return new SubagentRunResult(false, "子 Agent 执行失败", io.imiocode.agent.AgentStopReason.ERROR,
                    usage.get(), traceId);
        }
    }
}
