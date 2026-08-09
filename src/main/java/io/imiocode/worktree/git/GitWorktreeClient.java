package io.imiocode.worktree.git;

import io.imiocode.worktree.WorktreeException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** WorktreeManager 所需的最小 Git porcelain 接口。 */
public final class GitWorktreeClient {
    private final Path repositoryRoot;
    private final GitCommandRunner runner;

    public GitWorktreeClient(Path repositoryRoot, GitCommandRunner runner) {
        this.repositoryRoot = Objects.requireNonNull(repositoryRoot).toAbsolutePath().normalize();
        this.runner = Objects.requireNonNull(runner);
        Path actual = Path.of(runner.checked(this.repositoryRoot,
                List.of("rev-parse", "--show-toplevel"), "当前目录不是 Git 仓库"))
                .toAbsolutePath().normalize();
        if (!samePath(actual, this.repositoryRoot)) throw new WorktreeException("工作区必须是 Git 仓库根目录");
    }

    public Path repositoryRoot() { return repositoryRoot; }
    public String head() { return checked("无法读取当前 HEAD", "rev-parse", "HEAD"); }

    public String branch() {
        GitCommandResult result = run("symbolic-ref", "--quiet", "--short", "HEAD");
        return result.success() ? result.output().strip() : "HEAD";
    }

    public boolean branchExists(String branch) {
        return run("show-ref", "--verify", "--quiet", "refs/heads/" + branch).success();
    }

    public void addNew(Path path, String branch, String startPoint) {
        checked("无法创建 Worktree", "worktree", "add", "-b", branch,
                path.toAbsolutePath().normalize().toString(), startPoint);
    }

    public void addExisting(Path path, String branch) {
        checked("无法挂载已有 Worktree 分支", "worktree", "add",
                path.toAbsolutePath().normalize().toString(), branch);
    }

    public void removeForce(Path path) {
        checked("无法删除 Worktree", "worktree", "remove", "--force",
                path.toAbsolutePath().normalize().toString());
    }

    public void deleteBranch(String branch) { checked("无法删除 Worktree 分支", "branch", "-D", branch); }
    public void prune() { checked("无法清理 Git Worktree 元数据", "worktree", "prune"); }

    public String statusPorcelain(Path worktree) {
        return runner.checked(worktree, List.of("status", "--porcelain=v1", "--untracked-files=all"),
                "无法检查 Worktree 状态");
    }

    public int uniqueCommitCount(String originalHead, String branch) {
        String value = checked("无法检查 Worktree 提交", "rev-list", "--count", originalHead + ".." + branch);
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw new WorktreeException("Git 返回了无效的提交计数", exception); }
    }

    public boolean branchContainedByRemote(String branch) {
        GitCommandResult result = run("branch", "-r", "--contains", branch);
        return result.success() && !result.output().isBlank();
    }

    public boolean isIgnored(Path relativePath) {
        if (relativePath.isAbsolute() || relativePath.normalize().startsWith("..")) return false;
        return run("check-ignore", "--quiet", "--", relativePath.toString()).success();
    }

    public void configureWorktreeHooks(Path worktree, Path hooksPath) {
        checked("无法启用 Worktree 独立 Git 配置", "config", "extensions.worktreeConfig", "true");
        runner.checked(worktree, List.of("config", "--worktree", "core.hooksPath",
                hooksPath.toAbsolutePath().normalize().toString()), "无法配置 Worktree Git hooks");
    }

    public List<GitWorktreeEntry> list() {
        String output = checked("无法列出 Git Worktree", "worktree", "list", "--porcelain");
        List<GitWorktreeEntry> entries = new ArrayList<>();
        Path path = null; String head = ""; String branch = ""; boolean bare = false; boolean prunable = false;
        for (String line : (output + "\n").split("\\R", -1)) {
            if (line.isBlank()) {
                if (path != null) entries.add(new GitWorktreeEntry(path, head, branch, bare, prunable));
                path = null; head = ""; branch = ""; bare = false; prunable = false; continue;
            }
            if (line.startsWith("worktree ")) path = Path.of(line.substring(9)).toAbsolutePath().normalize();
            else if (line.startsWith("HEAD ")) head = line.substring(5).trim();
            else if (line.startsWith("branch refs/heads/")) branch = line.substring("branch refs/heads/".length()).trim();
            else if (line.equals("bare")) bare = true;
            else if (line.startsWith("prunable")) prunable = true;
        }
        return List.copyOf(entries);
    }

    private GitCommandResult run(String... args) { return runner.run(repositoryRoot, List.of(args)); }
    private String checked(String safeFailure, String... args) {
        return runner.checked(repositoryRoot, List.of(args), safeFailure);
    }

    private static boolean samePath(Path left, Path right) {
        try { return java.nio.file.Files.isSameFile(left, right); }
        catch (java.io.IOException exception) { return left.equals(right); }
    }
}
