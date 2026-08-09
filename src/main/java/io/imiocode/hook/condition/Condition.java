package io.imiocode.hook.condition;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record Condition(String field, ConditionOperator operator, String expected,
                        Optional<Pattern> compiledRegex,
                        Optional<HookGlobPattern> compiledGlob) {
    public Condition {
        field = Objects.requireNonNull(field, "field");
        operator = Objects.requireNonNull(operator, "operator");
        expected = Objects.requireNonNull(expected, "expected");
        compiledRegex = Objects.requireNonNullElse(compiledRegex, Optional.empty());
        compiledGlob = Objects.requireNonNullElse(compiledGlob, Optional.empty());
    }
}
