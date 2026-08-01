package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;

import java.util.List;
import java.util.Objects;

public record ContextResult(
        List<ChatMessage> committedHistory,
        List<ChatMessage> trajectory,
        List<ChatMessage> workingMessages,
        long beforeTokens,
        long afterTokens,
        int spilledResults,
        boolean compacted,
        ContextOutcome outcome) {
    public ContextResult {
        committedHistory = committedHistory == null ? List.of() : List.copyOf(committedHistory);
        trajectory = trajectory == null ? List.of() : List.copyOf(trajectory);
        workingMessages = workingMessages == null ? List.of() : List.copyOf(workingMessages);
        if (beforeTokens < 0 || afterTokens < 0 || spilledResults < 0) {
            throw new IllegalArgumentException("上下文统计不能为负数");
        }
        outcome = Objects.requireNonNull(outcome, "outcome");
    }
}
