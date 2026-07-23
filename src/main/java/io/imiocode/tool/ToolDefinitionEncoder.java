package io.imiocode.tool;

@FunctionalInterface
public interface ToolDefinitionEncoder<T> {
    T encode(ToolDefinition definition);
}
