package io.imiocode.tool;

/** 将统一工具定义编码为特定 Provider 或协议所需的结构。 */
@FunctionalInterface
public interface ToolDefinitionEncoder<T> {
    T encode(ToolDefinition definition);
}
