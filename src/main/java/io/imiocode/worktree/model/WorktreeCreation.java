package io.imiocode.worktree.model;

import java.util.List;

public record WorktreeCreation(WorktreeSession session, List<String> warnings) {
    public WorktreeCreation { warnings = List.copyOf(warnings == null ? List.of() : warnings); }
}
