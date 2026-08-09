package io.imiocode.hook.action;

public sealed interface Action permits CommandAction, PromptAction, HttpAction, AgentAction {
    HookActionType type();
}
