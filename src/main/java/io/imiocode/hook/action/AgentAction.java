package io.imiocode.hook.action;

public record AgentAction(String prompt) implements Action {
    public AgentAction {
        if (prompt == null || prompt.isBlank()) throw new IllegalArgumentException("agent prompt 不能为空");
        prompt = prompt.trim();
    }
    @Override public HookActionType type() { return HookActionType.AGENT; }
}
