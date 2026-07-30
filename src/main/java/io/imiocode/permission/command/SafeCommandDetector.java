package io.imiocode.permission.command;

/** 判断完整命令是否能作为确定性的安全只读命令自动放行。 */
@FunctionalInterface
public interface SafeCommandDetector {
    SafeCommandResult inspect(String command);
}
