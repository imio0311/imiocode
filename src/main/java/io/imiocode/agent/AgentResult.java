package io.imiocode.agent;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.ChatResponse;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Agent 任务的最终结果。
 */
public record AgentResult(
        AgentStopReason stopReason,
        List<ChatMessage> committedHistory,
        List<ChatMessage> trajectory,
        Optional<ChatResponse> finalResponse,
        boolean toolsExecuted,
        boolean sideEffectsPossible,
        Optional<AgentError> error
) {
    public AgentResult {
        stopReason = Objects.requireNonNull(stopReason, "stopReason 不能为空");
        committedHistory = List.copyOf(Objects.requireNonNull(committedHistory, "committedHistory 不能为空"));
        trajectory = List.copyOf(Objects.requireNonNull(trajectory, "trajectory 不能为空"));
        finalResponse = Objects.requireNonNullElse(finalResponse, Optional.empty());
        error = Objects.requireNonNullElse(error, Optional.empty());

        if (stopReason == AgentStopReason.FINAL_RESPONSE) {
            if (finalResponse.isEmpty() || error.isPresent()) {
                throw new IllegalArgumentException("正常完成必须包含 finalResponse 且不能包含 error");
            }
        } else if (stopReason == AgentStopReason.ERROR) {
            if (error.isEmpty() || finalResponse.isPresent()) {
                throw new IllegalArgumentException("错误结果必须包含 error 且不能包含 finalResponse");
            }
        } else if (finalResponse.isPresent() || error.isPresent()) {
            throw new IllegalArgumentException("非完成、非错误结果不能包含 finalResponse 或 error");
        }
    }

    public boolean completed() {
        return stopReason == AgentStopReason.FINAL_RESPONSE;
    }

    public static AgentResult completed(
            List<ChatMessage> committedHistory,
            List<ChatMessage> trajectory,
            ChatResponse response,
            boolean toolsExecuted,
            boolean sideEffectsPossible
    ) {
        return new AgentResult(
                AgentStopReason.FINAL_RESPONSE,
                committedHistory,
                trajectory,
                Optional.of(response),
                toolsExecuted,
                sideEffectsPossible,
                Optional.empty()
        );
    }

    public static AgentResult completed(List<ChatMessage> trajectory, ChatResponse response,
                                        boolean toolsExecuted, boolean sideEffectsPossible) {
        return completed(List.of(), trajectory, response, toolsExecuted, sideEffectsPossible);
    }

    public static AgentResult stopped(
            AgentStopReason reason,
            List<ChatMessage> committedHistory,
            List<ChatMessage> trajectory,
            boolean toolsExecuted,
            boolean sideEffectsPossible
    ) {
        if (reason == AgentStopReason.FINAL_RESPONSE || reason == AgentStopReason.ERROR) {
            throw new IllegalArgumentException("请使用对应的 completed 或 failed 工厂方法");
        }
        return new AgentResult(
                reason,
                committedHistory,
                trajectory,
                Optional.empty(),
                toolsExecuted,
                sideEffectsPossible,
                Optional.empty()
        );
    }

    public static AgentResult stopped(AgentStopReason reason, List<ChatMessage> trajectory,
                                      boolean toolsExecuted, boolean sideEffectsPossible) {
        return stopped(reason, List.of(), trajectory, toolsExecuted, sideEffectsPossible);
    }

    public static AgentResult failed(
            List<ChatMessage> committedHistory,
            List<ChatMessage> trajectory,
            boolean toolsExecuted,
            boolean sideEffectsPossible,
            AgentError error
    ) {
        return new AgentResult(
                AgentStopReason.ERROR,
                committedHistory,
                trajectory,
                Optional.empty(),
                toolsExecuted,
                sideEffectsPossible,
                Optional.of(error)
        );
    }

    public static AgentResult failed(List<ChatMessage> trajectory, boolean toolsExecuted,
                                     boolean sideEffectsPossible, AgentError error) {
        return failed(List.of(), trajectory, toolsExecuted, sideEffectsPossible, error);
    }
}
