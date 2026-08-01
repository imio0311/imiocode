package io.imiocode.context;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.tool.ToolSelection;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** 一次上下文管理请求，显式保留已提交历史与当前未完成轨迹的边界。 */
public record ContextRequest(
        List<ChatMessage> committedHistory,
        List<ChatMessage> trajectory,
        List<SystemReminder> reminders,
        ToolSelection toolSelection,
        OptionalInt outputTokenLimit,
        ContextManageMode mode,
        AutoCompactTrackingState trackingState) {
    public ContextRequest {
        committedHistory = committedHistory == null ? List.of() : List.copyOf(committedHistory);
        trajectory = trajectory == null ? List.of() : List.copyOf(trajectory);
        reminders = reminders == null ? List.of() : List.copyOf(reminders);
        toolSelection = toolSelection == null ? ToolSelection.allEnabled() : toolSelection;
        outputTokenLimit = outputTokenLimit == null ? OptionalInt.empty() : outputTokenLimit;
        mode = Objects.requireNonNull(mode, "mode");
        trackingState = Objects.requireNonNull(trackingState, "trackingState");
        if (committedHistory.isEmpty() && trajectory.isEmpty() && mode != ContextManageMode.FORCE) {
            throw new IllegalArgumentException("上下文消息不能为空");
        }
    }
}
