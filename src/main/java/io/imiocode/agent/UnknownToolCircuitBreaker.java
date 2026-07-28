package io.imiocode.agent;

import io.imiocode.tool.ToolAvailability;
import io.imiocode.tool.ToolResolution;

import java.util.Objects;

/**
 * 统计同一 Agent 任务中连续请求未知工具的次数。
 */
public final class UnknownToolCircuitBreaker {
    private static final int THRESHOLD = 3;
    private int committedCount;

    public synchronized Attempt beginAttempt() {
        return new Attempt(committedCount);
    }

    public final class Attempt {
        private int count;
        private boolean open;
        private boolean committed;

        private Attempt(int count) {
            this.count = count;
        }

        public synchronized CircuitObservation observe(ToolResolution resolution) {
            Objects.requireNonNull(resolution, "resolution 不能为空");
            if (open) {
                return CircuitObservation.OPENED;
            }
            ToolAvailability availability = resolution.availability();
            if (availability == ToolAvailability.UNKNOWN) {
                count++;
                if (count >= THRESHOLD) {
                    open = true;
                    return CircuitObservation.OPENED;
                }
                return CircuitObservation.INCREMENTED;
            }
            if (availability == ToolAvailability.AVAILABLE) {
                boolean changed = count != 0;
                count = 0;
                return changed ? CircuitObservation.RESET : CircuitObservation.UNCHANGED;
            }
            return CircuitObservation.UNCHANGED;
        }

        public synchronized boolean open() {
            return open;
        }

        public void commit() {
            int value;
            synchronized (this) {
                if (committed) {
                    throw new IllegalStateException("熔断尝试已经提交");
                }
                committed = true;
                value = count;
            }
            synchronized (UnknownToolCircuitBreaker.this) {
                committedCount = value;
            }
        }
    }
}
