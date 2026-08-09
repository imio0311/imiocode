package io.imiocode.hook.condition;

import io.imiocode.hook.HookContext;

public interface ConditionEvaluator {
    boolean matches(ConditionGroup group, HookContext context);
}
