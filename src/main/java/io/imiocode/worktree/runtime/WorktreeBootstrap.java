package io.imiocode.worktree.runtime;

import io.imiocode.worktree.WorktreeException;
import io.imiocode.worktree.lifecycle.WorktreeManager;

import java.nio.file.Path;

/** 在应用装配前决定本次真实工作区。 */
public final class WorktreeBootstrap {
    public WorktreeStartup resolve(Path launchDirectory, LaunchOptions options, WorktreeManager manager) {
        Path launch = launchDirectory.toAbsolutePath().normalize();
        if (options.resume()) {
            if (manager == null) throw new WorktreeException("当前目录不属于可恢复的 Git 仓库");
            return new WorktreeStartup(manager.resume().worktreePath(), false);
        }
        return new WorktreeStartup(launch, manager != null && manager.hasRecoverableSession());
    }
}
