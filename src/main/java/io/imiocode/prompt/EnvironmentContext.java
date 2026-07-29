package io.imiocode.prompt;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Objects;

/** 一次用户任务内保持不变的环境快照。 */
public record EnvironmentContext(
        Path workspace,
        String operatingSystem,
        String shell,
        ZonedDateTime capturedAt,
        GitContext git
) {
    public EnvironmentContext {
        workspace = Objects.requireNonNull(workspace, "工作区不能为空")
                .toAbsolutePath()
                .normalize();
        operatingSystem = requireText(operatingSystem, "操作系统");
        shell = requireText(shell, "Shell");
        capturedAt = Objects.requireNonNull(capturedAt, "采集时间不能为空");
        git = Objects.requireNonNull(git, "Git 上下文不能为空");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
