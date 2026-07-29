package io.imiocode.agent;

import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.tool.ToolSelection;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Plan Mode 的请求级策略，不修改全局工具注册状态。
 */
public final class PlanModePrompt {
    public static final Set<String> READ_ONLY_TOOLS =
            Set.of("read_file", "glob", "grep");

    private static final SystemReminder FULL_PLAN_REMINDER = new SystemReminder(
            ReminderScope.ROUND, """
            当前处于 Plan Mode。请只调查和分析现有项目，不要修改文件或执行会产生副作用的操作。
            使用可用的只读工具收集充分信息，最终输出一份清晰、可执行的实施计划。
            """);
    private static final SystemReminder CONCISE_PLAN_REMINDER = new SystemReminder(
            ReminderScope.ROUND,
            "保持 Plan Mode：只读调查，不做修改；信息充分后输出可执行计划。");
    private static final SystemReminder EXIT_PLAN_REMINDER = new SystemReminder(
            ReminderScope.ROUND,
            "已退出 Plan Mode，当前恢复普通执行模式；可以根据用户任务修改文件并执行必要命令。");

    private PlanModePrompt() {
    }

    public static ToolSelection toolSelection(AgentMode mode) {
        return mode == AgentMode.PLAN
                ? ToolSelection.only(READ_ONLY_TOOLS)
                : ToolSelection.allEnabled();
    }

    public static List<SystemReminder> additionalReminders(AgentMode mode) {
        return reminder(mode, 1).map(List::of).orElseGet(List::of);
    }

    public static Optional<SystemReminder> reminder(
            AgentMode mode,
            int iteration
    ) {
        if (mode == null) {
            throw new IllegalArgumentException("Agent Mode 不能为空");
        }
        if (iteration <= 0) {
            throw new IllegalArgumentException("iteration 必须为正数");
        }
        if (mode != AgentMode.PLAN) {
            return Optional.empty();
        }
        return Optional.of((iteration - 1) % 5 == 0
                ? FULL_PLAN_REMINDER
                : CONCISE_PLAN_REMINDER);
    }

    public static SystemReminder exitReminder() {
        return EXIT_PLAN_REMINDER;
    }
}
