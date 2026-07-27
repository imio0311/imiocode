package io.imiocode.agent;

import io.imiocode.llm.LlmClient;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单次 Agent 任务的并发状态和唯一终态竞争点。
 */
final class AgentTaskContext {
    private final long deadlineNanos;
    private final AtomicReference<AgentStopReason> stopReason = new AtomicReference<>();
    private final AtomicReference<ToolBatchExecutor> toolExecutor = new AtomicReference<>();
    private final AtomicBoolean toolsExecuted = new AtomicBoolean();
    private final AtomicBoolean sideEffectsPossible = new AtomicBoolean();

    AgentTaskContext(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout 不能为空");
        this.deadlineNanos = System.nanoTime() + timeout.toNanos();
    }

    boolean deadlineReached() {
        return System.nanoTime() - deadlineNanos >= 0;
    }

    boolean tryFinish(AgentStopReason reason) {
        return stopReason.compareAndSet(null, Objects.requireNonNull(reason, "reason 不能为空"));
    }

    boolean requestStop(AgentStopReason reason, LlmClient client) {
        if (reason == AgentStopReason.FINAL_RESPONSE || reason == AgentStopReason.ERROR) {
            throw new IllegalArgumentException("外部停止只能使用轮数、超时或取消原因");
        }
        if (!tryFinish(reason)) {
            return false;
        }
        Objects.requireNonNull(client, "client 不能为空").cancelActiveRequest();
        ToolBatchExecutor executor = toolExecutor.get();
        if (executor != null) {
            executor.cancel();
        }
        return true;
    }

    Optional<AgentStopReason> stopReason() {
        return Optional.ofNullable(stopReason.get());
    }

    void attachToolExecutor(ToolBatchExecutor executor) {
        toolExecutor.set(Objects.requireNonNull(executor, "executor 不能为空"));
        if (stopReason.get() != null) {
            executor.cancel();
        }
    }

    void markToolsExecuted() {
        toolsExecuted.set(true);
    }

    void markSideEffectsPossible() {
        sideEffectsPossible.set(true);
    }

    boolean toolsExecuted() {
        return toolsExecuted.get();
    }

    boolean sideEffectsPossible() {
        return sideEffectsPossible.get();
    }
}
