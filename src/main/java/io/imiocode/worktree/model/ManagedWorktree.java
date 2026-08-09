package io.imiocode.worktree.model;

import java.nio.file.Path;

public record ManagedWorktree(String slug, Path path, String branch, String head,
                              boolean active, WorktreeChangeSummary changes) { }
