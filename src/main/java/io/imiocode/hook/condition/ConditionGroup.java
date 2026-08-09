package io.imiocode.hook.condition;

import java.util.List;
import java.util.Objects;

public record ConditionGroup(ConditionConnector connector, List<Condition> conditions) {
    public ConditionGroup {
        connector = Objects.requireNonNull(connector, "connector");
        conditions = List.copyOf(Objects.requireNonNull(conditions, "conditions"));
        if (conditions.isEmpty()) throw new IllegalArgumentException("conditions 不能为空");
    }
}
