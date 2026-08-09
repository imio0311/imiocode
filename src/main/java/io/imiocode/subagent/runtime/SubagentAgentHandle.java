package io.imiocode.subagent.runtime;

import io.imiocode.agent.Agent;

import java.util.Optional;

public record SubagentAgentHandle(Agent agent, String model, Optional<String> warning) implements AutoCloseable {
    public SubagentAgentHandle {
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model 不能为空");
    }
    @Override public void close() { agent.close(); }
}
