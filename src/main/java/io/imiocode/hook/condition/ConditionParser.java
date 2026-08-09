package io.imiocode.hook.condition;

public interface ConditionParser {
    ConditionGroup parse(String expression) throws ConditionParseException;
}
