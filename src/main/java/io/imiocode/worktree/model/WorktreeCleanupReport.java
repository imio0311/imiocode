package io.imiocode.worktree.model;

import java.nio.file.Path;
import java.util.List;

public record WorktreeCleanupReport(boolean removed, Path path, String branch,
                                    String reason, List<String> warnings) {
    public WorktreeCleanupReport { warnings = List.copyOf(warnings == null ? List.of() : warnings); }
    public static WorktreeCleanupReport removed(Path path, String branch, List<String> warnings) {
        return new WorktreeCleanupReport(true, path, branch, "已安全清理", warnings);
    }
    public static WorktreeCleanupReport retained(Path path, String branch, String reason) {
        return new WorktreeCleanupReport(false, path, branch, reason, List.of());
    }
}
