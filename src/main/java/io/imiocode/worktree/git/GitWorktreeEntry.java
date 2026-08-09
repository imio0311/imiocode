package io.imiocode.worktree.git;

import java.nio.file.Path;

public record GitWorktreeEntry(Path path, String head, String branch, boolean bare, boolean prunable) { }
