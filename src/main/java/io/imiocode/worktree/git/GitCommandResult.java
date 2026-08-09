package io.imiocode.worktree.git;

public record GitCommandResult(int exitCode, String output) {
    public boolean success() { return exitCode == 0; }
}
