package io.imiocode.worktree.runtime;

import java.nio.file.Path;

public record WorktreeStartup(Path workspace, boolean pendingResumeNotice) {
    public WorktreeStartup { workspace = workspace.toAbsolutePath().normalize(); }
}
