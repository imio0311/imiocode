package io.imiocode.subagent.runtime;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.subagent.definition.AgentDefinition;

import java.util.List;

public interface SubagentRunner {
    SubagentRunResult run(AgentDefinition definition, String task, List<ChatMessage> parentHistory,
                          boolean background, SubagentRunMode mode,
                          CancellationRegistration cancellation);

    interface CancellationRegistration { void register(Runnable cancellation); }
}
