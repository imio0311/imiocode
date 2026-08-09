package io.imiocode.worktree.runtime;

import io.imiocode.worktree.WorktreeException;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/** Slash Command 与顶层运行时循环之间的一次性切换信号。 */
public final class WorkspaceTransitionController {
    private final AtomicReference<WorkspaceTransition> requested =
            new AtomicReference<>(new WorkspaceTransition.Stay());

    public void enter(Path path) { set(new WorkspaceTransition.Enter(normalize(path))); }
    public void exit(Path path) { set(new WorkspaceTransition.Exit(normalize(path))); }
    public WorkspaceTransition current() { return requested.get(); }

    private void set(WorkspaceTransition transition) {
        WorkspaceTransition current = requested.get();
        if (!(current instanceof WorkspaceTransition.Stay)
                || !requested.compareAndSet(current, transition)) {
            throw new WorktreeException("已有待处理的工作区切换");
        }
    }
    private static Path normalize(Path path) { return path.toAbsolutePath().normalize(); }
}
