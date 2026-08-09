package io.imiocode.worktree.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/** 手动 Enter 的可恢复状态；子 Agent 不写入该全局 session。 */
public record WorktreeSession(
        String sessionId,
        String slug,
        Path originalCwd,
        Path worktreePath,
        String worktreeBranch,
        String originalBranch,
        String originalHead,
        Instant createdAt) {
    public WorktreeSession {
        sessionId = require(sessionId, "sessionId");
        slug = require(slug, "slug");
        originalCwd = Objects.requireNonNull(originalCwd).toAbsolutePath().normalize();
        worktreePath = Objects.requireNonNull(worktreePath).toAbsolutePath().normalize();
        worktreeBranch = require(worktreeBranch, "worktreeBranch");
        originalBranch = require(originalBranch, "originalBranch");
        originalHead = require(originalHead, "originalHead");
        createdAt = Objects.requireNonNull(createdAt);
    }
    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 不能为空");
        return value.trim();
    }
}
