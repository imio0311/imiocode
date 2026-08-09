package io.imiocode.worktree.runtime;

import java.nio.file.Path;

public sealed interface WorkspaceTransition {
    record Stay() implements WorkspaceTransition { }
    record Enter(Path path) implements WorkspaceTransition { }
    record Exit(Path path) implements WorkspaceTransition { }
}
