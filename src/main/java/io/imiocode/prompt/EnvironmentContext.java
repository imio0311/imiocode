package io.imiocode.prompt;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/** 一次用户任务内保持不变的环境快照。 */
public record EnvironmentContext(
        Path workspace,
        String operatingSystem,
        String architecture,
        String shell,
        ZonedDateTime capturedAt,
        GitContext git,
        String model
) {
    public EnvironmentContext {
        workspace = Objects.requireNonNull(workspace, "工作区不能为空")
                .toAbsolutePath()
                .normalize();
        operatingSystem = requireText(operatingSystem, "操作系统");
        architecture = requireText(architecture, "系统架构");
        shell = requireText(shell, "Shell");
        capturedAt = Objects.requireNonNull(capturedAt, "采集时间不能为空");
        git = Objects.requireNonNull(git, "Git 上下文不能为空");
        model = requireText(model, "模型");
    }

    public EnvironmentContext(
            Path workspace,
            String operatingSystem,
            String shell,
            ZonedDateTime capturedAt,
            GitContext git
    ) {
        this(
                workspace,
                operatingSystem,
                "unknown",
                shell,
                capturedAt,
                git,
                "unknown");
    }

    public Optional<Boolean> isGitRepository() {
        return switch (git.state()) {
            case CLEAN, DIRTY -> Optional.of(true);
            case NOT_REPOSITORY -> Optional.of(false);
            case UNAVAILABLE -> Optional.empty();
        };
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
