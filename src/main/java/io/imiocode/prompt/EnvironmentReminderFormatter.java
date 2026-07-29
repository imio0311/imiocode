package io.imiocode.prompt;

import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** 将环境快照转换为字段顺序稳定的提醒消息。 */
public final class EnvironmentReminderFormatter {
    public SystemReminder format(EnvironmentContext context) {
        EnvironmentContext checked = Objects.requireNonNull(context, "环境上下文不能为空");
        String branch = checked.git().branch().orElse("不可用");
        String gitState = switch (checked.git().state()) {
            case CLEAN -> "clean";
            case DIRTY -> "dirty";
            case NOT_REPOSITORY -> "not-repository";
            case UNAVAILABLE -> "unavailable";
        };
        String content = """
                当前任务环境：
                - 工作目录：%s
                - 操作系统：%s
                - Shell：%s
                - 当前时间：%s
                - 时区：%s
                - Git 分支：%s
                - Git 状态：%s
                """.formatted(
                checked.workspace(),
                checked.operatingSystem(),
                checked.shell(),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(checked.capturedAt()),
                checked.capturedAt().getZone().getId(),
                branch,
                gitState);
        return new SystemReminder(ReminderScope.ENVIRONMENT, content);
    }
}
