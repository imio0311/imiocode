package io.imiocode.context;

/** 每个 Agent 任务独享的自动摘要失败状态。 */
public final class AutoCompactTrackingState {
    private int consecutiveFailures;
    private boolean circuitOpen;

    public synchronized boolean recordFailure() {
        if (!circuitOpen) {
            consecutiveFailures++;
            circuitOpen = consecutiveFailures >= ContextPolicy.MAX_CONSECUTIVE_SUMMARY_FAILURES;
        }
        return circuitOpen;
    }

    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        circuitOpen = false;
    }

    public synchronized int consecutiveFailures() { return consecutiveFailures; }
    public synchronized boolean circuitOpen() { return circuitOpen; }
}
