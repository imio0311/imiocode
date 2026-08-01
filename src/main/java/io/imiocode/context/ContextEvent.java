package io.imiocode.context;

import java.util.Objects;

public sealed interface ContextEvent permits ContextEvent.Started, ContextEvent.ResultsOffloaded,
        ContextEvent.Completed, ContextEvent.Failed, ContextEvent.CircuitOpened {
    ContextManageMode mode();

    record Started(ContextManageMode mode, long beforeTokens) implements ContextEvent {
        public Started { Objects.requireNonNull(mode, "mode"); if (beforeTokens < 0) throw new IllegalArgumentException("beforeTokens 不能为负数"); }
    }
    record ResultsOffloaded(ContextManageMode mode, int count, long afterTokens) implements ContextEvent {
        public ResultsOffloaded { Objects.requireNonNull(mode, "mode"); if (count <= 0 || afterTokens < 0) throw new IllegalArgumentException("落盘统计无效"); }
    }
    record Completed(ContextManageMode mode, long beforeTokens, long afterTokens) implements ContextEvent {
        public Completed { Objects.requireNonNull(mode, "mode"); if (beforeTokens < 0 || afterTokens < 0) throw new IllegalArgumentException("Token 统计无效"); }
    }
    record Failed(ContextManageMode mode, String safeMessage) implements ContextEvent {
        public Failed { Objects.requireNonNull(mode, "mode"); if (safeMessage == null || safeMessage.isBlank()) throw new IllegalArgumentException("safeMessage 不能为空"); }
    }
    record CircuitOpened(ContextManageMode mode, int consecutiveFailures) implements ContextEvent {
        public CircuitOpened { Objects.requireNonNull(mode, "mode"); if (consecutiveFailures < ContextPolicy.MAX_CONSECUTIVE_SUMMARY_FAILURES) throw new IllegalArgumentException("未达到熔断阈值"); }
    }
}
