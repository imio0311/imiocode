package io.imiocode.config;

import java.time.Duration;
import java.util.Objects;

/** Agent 单个任务的循环、时长和安全工具并发预算。 */
public record AgentConfig(
        int maxIterations,
        Duration taskTimeout,
        int maxParallelTools) {
    public static final int DEFAULT_MAX_ITERATIONS = 20;
    public static final Duration DEFAULT_TASK_TIMEOUT = Duration.ofSeconds(600);
    public static final int DEFAULT_MAX_PARALLEL_TOOLS = 4;

    public AgentConfig {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("agent.max-iterations 必须为正数");
        }
        Objects.requireNonNull(taskTimeout, "taskTimeout");
        if (taskTimeout.isZero() || taskTimeout.isNegative()) {
            throw new IllegalArgumentException("agent.timeout-seconds 必须为正数");
        }
        if (maxParallelTools <= 0) {
            throw new IllegalArgumentException("agent.max-parallel-tools 必须为正数");
        }
    }

    public static AgentConfig defaults() {
        return new AgentConfig(
                DEFAULT_MAX_ITERATIONS,
                DEFAULT_TASK_TIMEOUT,
                DEFAULT_MAX_PARALLEL_TOOLS);
    }
}
