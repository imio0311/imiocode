package io.imiocode.hook.condition;

import io.imiocode.hook.HookContext;
import java.util.Objects;

public final class DefaultConditionEvaluator implements ConditionEvaluator {
    @Override public boolean matches(ConditionGroup group, HookContext context) {
        Objects.requireNonNull(group, "group"); Objects.requireNonNull(context, "context");
        if (group.connector() == ConditionConnector.AND) {
            for (Condition condition : group.conditions()) if (!matches(condition, context)) return false;
            return true;
        }
        for (Condition condition : group.conditions()) if (matches(condition, context)) return true;
        return false;
    }
    private static boolean matches(Condition condition, HookContext context) {
        String actual = context.resolveField(condition.field()).orElse("");
        return switch (condition.operator()) {
            case EQUALS -> actual.equals(condition.expected());
            case NOT_EQUALS -> !actual.equals(condition.expected());
            case REGEX -> condition.compiledRegex().orElseThrow().matcher(actual).find();
            case GLOB -> condition.compiledGlob().orElseThrow().matches(actual);
        };
    }
}
