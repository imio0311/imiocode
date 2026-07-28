package io.imiocode.agent;

import io.imiocode.conversation.ChatResponse;
import io.imiocode.tool.ToolExecution;

import java.util.List;
import java.util.Objects;

public record StreamingTurnResult(
        ChatResponse response,
        List<ToolExecution> toolExecutions,
        boolean toolsStarted
) {
    public StreamingTurnResult {
        response = Objects.requireNonNull(response, "response 不能为空");
        toolExecutions = List.copyOf(
                Objects.requireNonNull(toolExecutions, "toolExecutions 不能为空"));
    }
}
