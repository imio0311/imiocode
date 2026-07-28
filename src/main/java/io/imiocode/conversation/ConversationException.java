package io.imiocode.conversation;

import io.imiocode.agent.AgentError;
import io.imiocode.agent.AgentResult;
import io.imiocode.agent.AgentStopReason;
import io.imiocode.llm.LlmErrorType;
import io.imiocode.llm.LlmException;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 会话编排层对外暴露的安全异常。
 */
public final class ConversationException extends Exception {
    private final String safeMessage;
    private final boolean recoverable;
    private final AgentStopReason stopReason;
    private final boolean toolsExecuted;
    private final boolean sideEffectsPossible;
    private final Duration retryAfter;

    public ConversationException(
            String safeMessage,
            boolean recoverable,
            boolean interrupted,
            boolean toolsExecuted
    ) {
        this(
                safeMessage,
                recoverable,
                interrupted ? AgentStopReason.CANCELLED : AgentStopReason.ERROR,
                toolsExecuted,
                false,
                null,
                null
        );
    }

    public ConversationException(
            String safeMessage,
            boolean recoverable,
            boolean interrupted,
            boolean toolsExecuted,
            Throwable cause
    ) {
        this(
                safeMessage,
                recoverable,
                interrupted ? AgentStopReason.CANCELLED : AgentStopReason.ERROR,
                toolsExecuted,
                false,
                null,
                cause
        );
    }

    public ConversationException(
            String safeMessage,
            boolean recoverable,
            boolean interrupted,
            boolean toolsExecuted,
            Duration retryAfter,
            Throwable cause
    ) {
        this(
                safeMessage,
                recoverable,
                interrupted ? AgentStopReason.CANCELLED : AgentStopReason.ERROR,
                toolsExecuted,
                false,
                retryAfter,
                cause
        );
    }

    public ConversationException(
            String safeMessage,
            boolean recoverable,
            AgentStopReason stopReason,
            boolean toolsExecuted,
            boolean sideEffectsPossible,
            Duration retryAfter,
            Throwable cause
    ) {
        super(requireText(safeMessage), cause);
        this.safeMessage = safeMessage;
        this.recoverable = recoverable;
        this.stopReason = Objects.requireNonNull(stopReason, "stopReason");
        this.toolsExecuted = toolsExecuted;
        this.sideEffectsPossible = sideEffectsPossible;
        this.retryAfter = retryAfter;
    }

    public static ConversationException from(LlmException exception, boolean toolsExecuted) {
        Objects.requireNonNull(exception, "exception");
        return new ConversationException(
                exception.safeMessage(),
                exception.recoverable(),
                exception.type() == LlmErrorType.INTERRUPTED
                        ? AgentStopReason.CANCELLED
                        : AgentStopReason.ERROR,
                toolsExecuted,
                false,
                exception.retryAfter().orElse(null),
                exception
        );
    }

    public static ConversationException from(AgentResult result) {
        Objects.requireNonNull(result, "result");
        if (result.completed()) {
            throw new IllegalArgumentException("成功结果不能转换为 ConversationException");
        }
        AgentError error = result.error().orElse(null);
        String message = switch (result.stopReason()) {
            case MAX_ITERATIONS -> "已达到 Agent 最大循环轮数";
            case TIMEOUT -> "Agent 任务执行超时";
            case CANCELLED -> "Agent 任务已取消";
            case TOO_MANY_UNKNOWN_TOOLS -> "模型连续请求不存在的工具，Agent 已停止";
            case ERROR -> error == null ? "Agent 执行失败" : error.safeMessage();
            case FINAL_RESPONSE -> throw new IllegalArgumentException("成功结果不能转换为异常");
        };
        return new ConversationException(
                message,
                error != null && error.recoverable(),
                result.stopReason(),
                result.toolsExecuted(),
                result.sideEffectsPossible(),
                error == null ? null : error.retryAfter().orElse(null),
                null
        );
    }

    public String safeMessage() {
        return safeMessage;
    }

    public boolean recoverable() {
        return recoverable;
    }

    public boolean interrupted() {
        return stopReason == AgentStopReason.CANCELLED;
    }

    public AgentStopReason stopReason() {
        return stopReason;
    }

    public boolean toolsExecuted() {
        return toolsExecuted;
    }

    public boolean sideEffectsPossible() {
        return sideEffectsPossible;
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("safeMessage 不能为空");
        }
        return value;
    }
}
