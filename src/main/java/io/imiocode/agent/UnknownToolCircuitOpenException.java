package io.imiocode.agent;

/**
 * 仅用于在 Agent 内部尽快终止已熔断的流。
 */
public final class UnknownToolCircuitOpenException extends RuntimeException {
    public UnknownToolCircuitOpenException() {
        super("连续请求未知工具，已停止当前任务");
    }
}
