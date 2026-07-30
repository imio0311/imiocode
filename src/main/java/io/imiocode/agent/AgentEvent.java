package io.imiocode.agent;

import io.imiocode.llm.TokenUsage;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.permission.PermissionPrompt;
import io.imiocode.permission.PermissionReply;
import io.imiocode.tool.ToolExecutionEvent;

import java.util.Objects;
import java.time.Duration;

/**
 * Agent 向 UI 发布的领域事件。事件不会暴露 Provider 原始元数据。
 */
public sealed interface AgentEvent {

    record TaskStarted(AgentMode mode) implements AgentEvent {
        public TaskStarted {
            mode = Objects.requireNonNull(mode, "mode 不能为空");
        }
    }

    record IterationStarted(int iteration) implements AgentEvent {
        public IterationStarted {
            requirePositive(iteration, "iteration");
        }
    }

    record RetryScheduled(
            int iteration,
            int nextAttempt,
            LlmErrorType reason,
            Duration delay,
            int outputTokenLimit
    ) implements AgentEvent {
        public RetryScheduled {
            requirePositive(iteration, "iteration");
            if (nextAttempt < 2 || nextAttempt > 4) {
                throw new IllegalArgumentException("nextAttempt 必须在 2 到 4 之间");
            }
            reason = Objects.requireNonNull(reason, "reason 不能为空");
            delay = Objects.requireNonNull(delay, "delay 不能为空");
            if (delay.isNegative()) {
                throw new IllegalArgumentException("delay 不能为负数");
            }
            requirePositive(outputTokenLimit, "outputTokenLimit");
        }
    }

    record TextDelta(int iteration, String text) implements AgentEvent {
        public TextDelta {
            requirePositive(iteration, "iteration");
            text = Objects.requireNonNull(text, "text 不能为空");
        }
    }

    record ThinkingDelta(int iteration, String text) implements AgentEvent {
        public ThinkingDelta {
            requirePositive(iteration, "iteration");
            text = Objects.requireNonNull(text, "text 不能为空");
        }
    }

    record ThinkingCompleted(int iteration) implements AgentEvent {
        public ThinkingCompleted {
            requirePositive(iteration, "iteration");
        }
    }

    record ModelToolRequested(
            int iteration,
            int callIndex,
            String callId,
            String toolName
    ) implements AgentEvent {
        public ModelToolRequested {
            requirePositive(iteration, "iteration");
            requireNonNegative(callIndex, "callIndex");
            callId = requireText(callId, "callId");
            toolName = requireText(toolName, "toolName");
        }
    }

    record ModelResponseCompleted(
            int iteration,
            TokenUsage usage,
            boolean hasToolCalls
    ) implements AgentEvent {
        public ModelResponseCompleted {
            requirePositive(iteration, "iteration");
            usage = Objects.requireNonNull(usage, "usage 不能为空");
        }
    }

    record ToolBatchStarted(
            int iteration,
            int batchIndex,
            ToolBatchKind kind,
            int size
    ) implements AgentEvent {
        public ToolBatchStarted {
            requirePositive(iteration, "iteration");
            requireNonNegative(batchIndex, "batchIndex");
            kind = Objects.requireNonNull(kind, "kind 不能为空");
            requirePositive(size, "size");
        }
    }

    record ToolExecutionChanged(
            int iteration,
            int callIndex,
            ToolExecutionEvent execution
    ) implements AgentEvent {
        public ToolExecutionChanged {
            requirePositive(iteration, "iteration");
            requireNonNegative(callIndex, "callIndex");
            execution = Objects.requireNonNull(execution, "execution 不能为空");
        }
    }

    record ToolBatchCompleted(
            int iteration,
            int batchIndex,
            ToolBatchKind kind,
            int size
    ) implements AgentEvent {
        public ToolBatchCompleted {
            requirePositive(iteration, "iteration");
            requireNonNegative(batchIndex, "batchIndex");
            kind = Objects.requireNonNull(kind, "kind 不能为空");
            requirePositive(size, "size");
        }
    }

    record PermissionRequested(PermissionPrompt prompt) implements AgentEvent {
        public PermissionRequested {
            prompt = Objects.requireNonNull(prompt, "prompt 不能为空");
        }
    }

    record PermissionResolved(
            String requestId,
            PermissionReply reply
    ) implements AgentEvent {
        public PermissionResolved {
            requestId = requireText(requestId, "requestId");
            reply = Objects.requireNonNull(reply, "reply 不能为空");
        }
    }

    record ModeChanged(AgentMode previous, AgentMode current) implements AgentEvent {
        public ModeChanged {
            previous = Objects.requireNonNull(previous, "previous 不能为空");
            current = Objects.requireNonNull(current, "current 不能为空");
        }
    }

    record TaskCompleted(int iterations) implements AgentEvent {
        public TaskCompleted {
            requirePositive(iterations, "iterations");
        }
    }

    record TaskStopped(
            AgentStopReason reason,
            int iterations,
            boolean sideEffectsPossible
    ) implements AgentEvent {
        public TaskStopped {
            reason = Objects.requireNonNull(reason, "reason 不能为空");
            if (reason == AgentStopReason.FINAL_RESPONSE || reason == AgentStopReason.ERROR) {
                throw new IllegalArgumentException("TaskStopped 不能使用完成或错误原因");
            }
            requireNonNegative(iterations, "iterations");
        }
    }

    record TaskFailed(
            int iterations,
            AgentError error,
            boolean sideEffectsPossible
    ) implements AgentEvent {
        public TaskFailed {
            requireNonNegative(iterations, "iterations");
            error = Objects.requireNonNull(error, "error 不能为空");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " 不能为负数");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
