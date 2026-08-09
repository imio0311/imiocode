package io.imiocode.worktree.runtime;

import io.imiocode.worktree.config.WorktreeConfig;
import io.imiocode.worktree.git.GitCommandRunner;
import io.imiocode.worktree.lifecycle.WorktreeManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorktreeBootstrapTest {
    @TempDir Path temp;

    @Test void normalLaunchOnlyNoticesAndResumeSelectsRecordedWorktree() throws Exception {
        Path repo = temp.resolve("repo"); Files.createDirectories(repo);
        GitCommandRunner runner = new GitCommandRunner(Duration.ofSeconds(10));
        checked(runner, repo, "init"); checked(runner, repo, "config", "user.email", "test@example.invalid");
        checked(runner, repo, "config", "user.name", "Test");
        Files.writeString(repo.resolve("file.txt"), "root");
        Files.writeString(repo.resolve(".gitignore"), ".imiocode/\n");
        checked(runner, repo, "add", "file.txt", ".gitignore"); checked(runner, repo, "commit", "-m", "initial");
        try (WorktreeManager manager = new WorktreeManager(repo, WorktreeConfig.defaults())) {
            Path worktree = manager.create("resume-me").session().worktreePath();
            manager.enter("resume-me");
            WorktreeBootstrap bootstrap = new WorktreeBootstrap();
            WorktreeStartup normal = bootstrap.resolve(repo, new LaunchOptions(false), manager);
            assertEquals(repo.toAbsolutePath(), normal.workspace());
            assertTrue(normal.pendingResumeNotice());
            WorktreeStartup resumed = bootstrap.resolve(repo, new LaunchOptions(true), manager);
            assertEquals(worktree, resumed.workspace());
            assertFalse(resumed.pendingResumeNotice());
            manager.exit(true, true);
        }
    }

    private static void checked(GitCommandRunner runner, Path cwd, String... args) {
        var result = runner.run(cwd, List.of(args)); assertEquals(0, result.exitCode(), result.output());
    }
}
