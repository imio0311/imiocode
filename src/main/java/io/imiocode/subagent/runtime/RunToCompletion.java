package io.imiocode.subagent.runtime;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.subagent.context.SubagentContextBuilder;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.subagent.filter.SubagentToolFilter;
import io.imiocode.subagent.trace.TraceRegistry;
import io.imiocode.tool.ToolRegistry;

import java.util.List;
import io.imiocode.worktree.lifecycle.WorktreeManager;

/** CH13 对外命名的子 Agent 完整执行入口。 */
public final class RunToCompletion implements SubagentRunner {
    private final DefaultSubagentRunner delegate;
    public RunToCompletion(ToolRegistry tools, SubagentToolFilter filter,
                           SubagentContextBuilder contexts, SubagentAgentFactory agents,
                           TraceRegistry traces) {
        this.delegate = new DefaultSubagentRunner(tools, filter, contexts, agents, traces);
    }
    public RunToCompletion(ToolRegistry tools, SubagentToolFilter filter,
                           SubagentContextBuilder contexts, SubagentAgentFactory agents,
                           TraceRegistry traces, String parentTraceId) {
        this.delegate = new DefaultSubagentRunner(tools, filter, contexts, agents, traces, parentTraceId);
    }
    public RunToCompletion(ToolRegistry tools, SubagentToolFilter filter,
                           SubagentContextBuilder contexts, SubagentAgentFactory agents,
                           TraceRegistry traces, String parentTraceId, WorktreeManager worktrees) {
        this.delegate = new DefaultSubagentRunner(tools, filter, contexts, agents, traces, parentTraceId, worktrees);
    }
    @Override public SubagentRunResult run(AgentDefinition definition, String task,
            List<ChatMessage> parentHistory, boolean background, SubagentRunMode mode,
            CancellationRegistration cancellation) {
        return delegate.run(definition, task, parentHistory, background, mode, cancellation);
    }
}
