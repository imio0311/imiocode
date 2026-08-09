package io.imiocode.worktree.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitWorktreeClientTest {
    @TempDir Path temp;

    @Test void createsListsInspectsAndRemovesRealWorktree() throws Exception {
        Path repository = temp.resolve("repo with space");
        Files.createDirectories(repository);
        GitCommandRunner runner = new GitCommandRunner(Duration.ofSeconds(10));
        checked(runner, repository, "init");
        checked(runner, repository, "config", "user.email", "test@example.invalid");
        checked(runner, repository, "config", "user.name", "Test");
        Files.writeString(repository.resolve("README.md"), "root\n");
        checked(runner, repository, "add", "README.md");
        checked(runner, repository, "commit", "-m", "initial");

        GitWorktreeClient client = new GitWorktreeClient(repository, runner);
        Path worktree = repository.resolve(".imiocode/worktrees/demo");
        String originalHead = client.head();
        client.addNew(worktree, "worktree-demo", originalHead);
        assertTrue(Files.isDirectory(worktree));
        assertTrue(client.list().stream().anyMatch(item -> item.path().equals(worktree.toAbsolutePath())));
        assertTrue(client.statusPorcelain(worktree).isBlank());
        assertEquals(0, client.uniqueCommitCount(originalHead, "worktree-demo"));
        client.removeForce(worktree);
        client.deleteBranch("worktree-demo");
        assertFalse(Files.exists(worktree));
        assertFalse(client.branchExists("worktree-demo"));
    }

    private static void checked(GitCommandRunner runner, Path cwd, String... args) {
        GitCommandResult result = runner.run(cwd, List.of(args));
        assertEquals(0, result.exitCode(), result.output());
    }
}
