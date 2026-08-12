package io.imiocode.hook.condition;

import io.imiocode.hook.HookContext;

/** 在 Hook 上下文上计算已解析条件组的真假。 */
public interface ConditionEvaluator {
    boolean matches(ConditionGroup group, HookContext context);
}
