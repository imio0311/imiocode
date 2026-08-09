package io.imiocode.subagent.definition;

/** 同名 Agent 定义的覆盖优先级，数字越大优先级越高。 */
public enum AgentDefinitionSource {
    PLUGIN(0), BUILTIN(1), USER(2), PROJECT(3);

    private final int priority;
    AgentDefinitionSource(int priority) { this.priority = priority; }
    public int priority() { return priority; }
}
