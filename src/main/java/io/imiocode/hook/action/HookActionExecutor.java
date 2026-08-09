package io.imiocode.hook.action;

import io.imiocode.hook.HookActionResult;
import io.imiocode.hook.HookContext;

public interface HookActionExecutor<A extends Action> {
    HookActionType type();
    HookActionResult execute(A action, HookContext context);
}
