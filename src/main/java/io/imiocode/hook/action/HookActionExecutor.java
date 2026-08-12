package io.imiocode.hook.action;

import io.imiocode.hook.HookActionResult;
import io.imiocode.hook.HookContext;

/** 将一种 Hook 动作映射为受控的执行实现和统一结果。 */
public interface HookActionExecutor<A extends Action> {
    HookActionType type();
    HookActionResult execute(A action, HookContext context);
}
