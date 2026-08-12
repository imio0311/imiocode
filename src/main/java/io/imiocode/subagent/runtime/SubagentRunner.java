package io.imiocode.subagent.runtime;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.subagent.definition.AgentDefinition;

import java.util.List;

/**
 * 把已解析的 Agent 定义执行到终态。
 *
 * <p>调用方通过注册回调传播取消信号；后台执行不得等待交互式权限确认。</p>
 */
public interface SubagentRunner {
    SubagentRunResult run(AgentDefinition definition, String task, List<ChatMessage> parentHistory,
                          boolean background, SubagentRunMode mode,
                          CancellationRegistration cancellation);

    /** 接收底层运行时提供的幂等取消动作。 */
    interface CancellationRegistration { void register(Runnable cancellation); }
}
