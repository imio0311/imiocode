package io.imiocode.worktree.lifecycle;

import io.imiocode.worktree.WorktreeException;
import io.imiocode.worktree.git.GitWorktreeClient;
import io.imiocode.worktree.model.WorktreeChangeSummary;
import io.imiocode.worktree.model.WorktreeSession;
import io.imiocode.worktree.security.WorktreeNames;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

/** 删除前重新验证路径、Git 注册、分支和内容状态。 */
public final class WorktreeSafetyInspector {
    private final WorktreeNames names;
    private final GitWorktreeClient git;

    public WorktreeSafetyInspector(WorktreeNames names, GitWorktreeClient git) {
        this.names = Objects.requireNonNull(names); this.git = Objects.requireNonNull(git);
    }

    public WorktreeChangeSummary inspect(WorktreeSession session) {
        validateIdentity(session);
        String status = git.statusPorcelain(session.worktreePath());
        int changed = status.isBlank() ? 0 : (int) status.lines().filter(line -> !line.isBlank()).count();
        int commits = git.uniqueCommitCount(session.originalHead(), session.worktreeBranch());
        boolean remote = git.branchContainedByRemote(session.worktreeBranch());
        return new WorktreeChangeSummary(changed, commits, remote);
    }

    public void validateIdentity(WorktreeSession session) {
        String slug = names.slug(session.slug());
        Path expected = names.path(slug);
        String expectedBranch = names.branch(slug);
        if (!expected.equals(session.worktreePath().toAbsolutePath().normalize())
                || !expectedBranch.equals(session.worktreeBranch())) {
            throw new WorktreeException("Worktree 会话不属于受管目录或分支");
        }
        if (!Files.isDirectory(expected, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(expected)) {
            throw new WorktreeException("Worktree 目录不存在或不是安全目录");
        }
        try {
            Path real = expected.toRealPath(); Path managed = names.managedRoot().toRealPath();
            if (!real.startsWith(managed) || real.equals(managed)) throw new WorktreeException("Worktree 真实路径越界");
        } catch (java.io.IOException exception) { throw new WorktreeException("无法验证 Worktree 真实路径", exception); }
        boolean registered = git.list().stream().anyMatch(entry -> entry.path().equals(expected)
                && expectedBranch.equals(entry.branch()));
        if (!registered) throw new WorktreeException("Worktree 未在 Git 中注册或分支不匹配");
    }
}
