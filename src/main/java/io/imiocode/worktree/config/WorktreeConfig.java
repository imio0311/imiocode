package io.imiocode.worktree.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Git Worktree 的有界、安全配置。directory 始终是项目相对路径。 */
public record WorktreeConfig(
        Path directory,
        Duration gitTimeout,
        Duration staleAfter,
        Duration cleanupInterval,
        List<String> linkDirectories,
        List<String> copyIncludes,
        boolean copyLocalConfig) {

    public WorktreeConfig {
        directory = Objects.requireNonNull(directory, "directory").normalize();
        if (directory.isAbsolute() || directory.getNameCount() < 2
                || !".imiocode".equals(directory.getName(0).toString())
                || directory.toString().contains("..")) {
            throw new IllegalArgumentException("directory 必须位于项目 .imiocode 目录内");
        }
        gitTimeout = positive(gitTimeout, "gitTimeout");
        staleAfter = positive(staleAfter, "staleAfter");
        cleanupInterval = positive(cleanupInterval, "cleanupInterval");
        linkDirectories = safeRelativeNames(linkDirectories, "linkDirectories");
        copyIncludes = List.copyOf(Objects.requireNonNullElse(copyIncludes, List.of()));
        if (copyIncludes.stream().anyMatch(value -> value == null || value.isBlank()
                || value.startsWith("/") || value.startsWith("\\") || value.contains(".."))) {
            throw new IllegalArgumentException("copyIncludes 只能包含安全的项目相对 glob");
        }
    }

    public static WorktreeConfig defaults() {
        return new WorktreeConfig(Path.of(".imiocode", "worktrees"), Duration.ofSeconds(10),
                Duration.ofDays(7), Duration.ofHours(1),
                List.of("node_modules", ".venv", "venv", "vendor"), List.of(), true);
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) throw new IllegalArgumentException(name + " 必须为正数");
        return value;
    }

    private static List<String> safeRelativeNames(List<String> values, String name) {
        List<String> result = List.copyOf(Objects.requireNonNullElse(values, List.of()));
        for (String value : result) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 不能包含空值");
            Path path = Path.of(value).normalize();
            if (path.isAbsolute() || path.getNameCount() != 1 || ".".equals(path.toString())
                    || "..".equals(path.toString())) {
                throw new IllegalArgumentException(name + " 只能包含单段项目相对目录名");
            }
        }
        return result;
    }
}
