package io.imiocode.worktree.model;

public record WorktreeChangeSummary(int changedFiles, int uniqueCommits, boolean containedByRemote) {
    public WorktreeChangeSummary {
        if (changedFiles < 0 || uniqueCommits < 0) throw new IllegalArgumentException("计数不能为负数");
    }
    public boolean clean() { return changedFiles == 0 && uniqueCommits == 0; }
    public String summary() {
        return changedFiles + " 个文件变更，" + uniqueCommits + " 个独有提交";
    }
}
