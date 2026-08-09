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
import java.util.Optional;
import java.nio.file.Path;
import io.imiocode.subagent.definition.AgentIsolation;
import io.imiocode.worktree.lifecycle.WorktreeManager;
import io.imiocode.worktree.model.WorktreeLease;
import io.imiocode.worktree.model.WorktreeCleanupReport;

/** 独立运行一个子 Agent 到终态；权限确认在后台一律拒绝，避免隐藏任务阻塞。 */
public final class DefaultSubagentRunner implements SubagentRunner {
    private final ToolRegistry tools;
    private final SubagentToolFilter filter;
    private final SubagentContextBuilder contexts;
    private final SubagentAgentFactory agents;
    private final TraceRegistry traces;
    private final String parentTraceId;
    private final WorktreeManager worktrees;

    public DefaultSubagentRunner(ToolRegistry tools, SubagentToolFilter filter,
                                 SubagentContextBuilder contexts, SubagentAgentFactory agents,
                                 TraceRegistry traces) {
        this(tools, filter, contexts, agents, traces, null, null);
    }

    public DefaultSubagentRunner(ToolRegistry tools, SubagentToolFilter filter,
                                 SubagentContextBuilder contexts, SubagentAgentFactory agents,
                                 TraceRegistry traces, String parentTraceId) {
        this(tools, filter, contexts, agents, traces, parentTraceId, null);
    }

    public DefaultSubagentRunner(ToolRegistry tools, SubagentToolFilter filter,
                                 SubagentContextBuilder contexts, SubagentAgentFactory agents,
                                 TraceRegistry traces, String parentTraceId, WorktreeManager worktrees) {
        this.tools=Objects.requireNonNull(tools); this.filter=Objects.requireNonNull(filter);
        this.contexts=Objects.requireNonNull(contexts); this.agents=Objects.requireNonNull(agents);
        this.traces=Objects.requireNonNull(traces);
        this.parentTraceId=parentTraceId;
        this.worktrees=worktrees;
    }

    @Override public SubagentRunResult run(AgentDefinition definition, String task,
            List<ChatMessage> parentHistory, boolean background, SubagentRunMode mode,
            CancellationRegistration cancellation) {
        if (definition.isolation() == AgentIsolation.WORKTREE) {
            if (worktrees == null) return new SubagentRunResult(false, "Worktree isolation 尚未初始化",
                    io.imiocode.agent.AgentStopReason.ERROR, TraceTokenUsage.zero(), "unavailable");
            WorktreeLease lease;
            try { lease = worktrees.createAgentWorktree(definition.name()); }
            catch (RuntimeException exception) {
                return new SubagentRunResult(false, "无法创建子 Agent Worktree",
                        io.imiocode.agent.AgentStopReason.ERROR, TraceTokenUsage.zero(), "unavailable");
            }
            SubagentRunResult result;
            WorktreeCleanupReport cleanup;
            try {
                result = runScoped(definition, task, parentHistory, background, mode, cancellation,
                        Optional.of(lease.session()), lease.workdir());
            } catch (RuntimeException exception) {
                result = new SubagentRunResult(false, "子 Agent Worktree 执行失败",
                        io.imiocode.agent.AgentStopReason.ERROR, TraceTokenUsage.zero(), "unavailable");
            } finally {
                // Agent、LLM 或 Hook 任一环节失败，都必须释放 lease 和文件锁。
                cleanup = lease.closeSafely();
            }
            String lifecycle = cleanup.removed()
                    ? "\n[Worktree] 已安全清理 " + cleanup.branch()
                    : "\n[Worktree] 已保留\nPath: " + cleanup.path() + "\nBranch: " + cleanup.branch()
                            + "\nReason: " + cleanup.reason();
            return new SubagentRunResult(result.success(), result.output() + lifecycle,
                    result.stopReason(), result.usage(), result.traceId());
        }
        return runScoped(definition, task, parentHistory, background, mode, cancellation,
                Optional.empty(), null);
    }

    private SubagentRunResult runScoped(AgentDefinition definition, String task,
            List<ChatMessage> parentHistory, boolean background, SubagentRunMode mode,
            CancellationRegistration cancellation, Optional<io.imiocode.worktree.model.WorktreeSession> worktree,
            Path workdir) {
        ToolSelection selection = filter.select(definition, tools.enabledNames(), background);
        String traceId = null;
        AtomicReference<TraceTokenUsage> usage = new AtomicReference<>(TraceTokenUsage.zero());
        try (SubagentAgentHandle handle = workdir == null
                ? agents.create(definition, selection) : agents.create(definition, selection, workdir)) {
            final String startedTraceId = traces.start(definition.name(), parentTraceId, handle.model(), background);
            traceId = startedTraceId;
            cancellation.register(handle.agent()::cancelActive);
            AgentResult result = handle.agent().run(new AgentRequest(
                    contexts.history(definition, parentHistory, mode), contexts.taskMessage(task),
                    contexts.reminders(definition, worktree), selection), event -> {
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
