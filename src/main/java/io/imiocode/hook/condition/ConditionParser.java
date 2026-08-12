package io.imiocode.hook.condition;

/** 将受限 Hook 条件表达式解析为可预编译、可重复求值的条件组。 */
public interface ConditionParser {
    ConditionGroup parse(String expression) throws ConditionParseException;
}
