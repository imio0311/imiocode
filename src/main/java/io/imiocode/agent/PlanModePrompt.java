package io.imiocode.agent;

import io.imiocode.conversation.SystemReminder;
import io.imiocode.tool.ToolSelection;

import java.util.List;
import java.util.Set;

/**
 * Plan Mode 的请求级策略，不修改全局工具注册状态。
 */
public final class PlanModePrompt {
    public static final Set<String> READ_ONLY_TOOLS =
            Set.of("read_file", "glob", "grep");

    private static final SystemReminder PLAN_REMINDER = new SystemReminder("""
            当前处于 Plan Mode。请只调查和分析现有项目，不要修改文件或执行会产生副作用的操作。
            使用可用的只读工具收集充分信息，最终输出一份清晰、可执行的实施计划。
            """);

    private PlanModePrompt() {
    }

    public static ToolSelection toolSelection(AgentMode mode) {
        return mode == AgentMode.PLAN
                ? ToolSelection.only(READ_ONLY_TOOLS)
                : ToolSelection.allEnabled();
    }

    public static List<SystemReminder> additionalReminders(AgentMode mode) {
        return mode == AgentMode.PLAN ? List.of(PLAN_REMINDER) : List.of();
    }
}
