package io.imiocode.worktree.lifecycle;

import io.imiocode.worktree.WorktreeException;
import io.imiocode.worktree.config.WorktreeConfig;
import io.imiocode.worktree.git.GitCommandResult;
import io.imiocode.worktree.git.GitCommandRunner;
import io.imiocode.worktree.model.WorktreeLease;
import io.imiocode.worktree.model.WorktreeSession;
import io.imiocode.worktree.persistence.WorktreeSessionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.attribute.FileTime;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorktreeManagerTest {
    @TempDir Path temp;
    Path repository;
    GitCommandRunner runner;
    WorktreeManager manager;

    @BeforeEach void setUp() throws Exception {
        repository = temp.resolve("repo"); Files.createDirectories(repository);
        runner = new GitCommandRunner(Duration.ofSeconds(10));
        git(repository, "init"); git(repository, "config", "user.email", "test@example.invalid");
        git(repository, "config", "user.name", "Test");
        Files.writeString(repository.resolve("tracked.txt"), "root\n");
        Files.writeString(repository.resolve(".gitignore"), "config.yaml\n.imiocode/\n");
        git(repository, "add", "tracked.txt", ".gitignore"); git(repository, "commit", "-m", "initial");
        manager = new WorktreeManager(repository, WorktreeConfig.defaults());
    }

    @AfterEach void tearDown() { if (manager != null) manager.close(); }

    @Test void createEnterKeepExitAndRemoveCleanWorktree() {
        var created = manager.create("Demo");
        assertEquals("demo", created.session().slug());
        assertTrue(Files.isDirectory(created.session().worktreePath()));
        assertEquals(1, manager.list().size());
        assertEquals("demo", manager.enter("demo").slug());
        assertTrue(manager.hasRecoverableSession());
        assertFalse(manager.exit(false, false).removed());
        assertFalse(manager.hasRecoverableSession());
        assertTrue(manager.remove("demo", false).removed());
        assertTrue(manager.list().isEmpty());
    }

    @Test void dirtyAndCommittedWorktreesRequireExplicitDiscard() throws Exception {
        var dirty = manager.create("dirty").session();
        Files.writeString(dirty.worktreePath().resolve("tracked.txt"), "changed\n");
        assertThrows(WorktreeException.class, () -> manager.remove("dirty", false));
        assertTrue(manager.remove("dirty", true).removed());

        var committed = manager.create("committed").session();
        Files.writeString(committed.worktreePath().resolve("tracked.txt"), "commit\n");
        git(committed.worktreePath(), "add", "tracked.txt");
        git(committed.worktreePath(), "commit", "-m", "worktree change");
        assertThrows(WorktreeException.class, () -> manager.remove("committed", false));
        assertTrue(manager.remove("committed", true).removed());
    }

    @Test void forgedSessionPathFailsClosedWithoutDeletingOutsideDirectory() throws Exception {
        manager.create("guarded");
        WorktreeSession valid = manager.enter("guarded");
        Path outside = temp.resolve("outside");
        Files.createDirectories(outside);
        Files.writeString(outside.resolve("keep.txt"), "keep\n");
        WorktreeSession forged = new WorktreeSession(valid.sessionId(), valid.slug(),
                valid.originalCwd(), outside, valid.worktreeBranch(), valid.originalBranch(),
                valid.originalHead(), valid.createdAt());
        manager.sessionStore().save(forged);

        assertThrows(WorktreeException.class, () -> manager.exit(true, true));
        assertEquals("keep\n", Files.readString(outside.resolve("keep.txt")));

        manager.sessionStore().save(valid);
        assertTrue(manager.exit(true, true).removed());
    }

    @Test void agentLeaseCleansOnlySafeWorktrees() throws Exception {
        WorktreeLease clean = manager.createAgentWorktree("explore");
        Path cleanPath = clean.workdir();
        assertTrue(clean.closeSafely().removed());
        assertFalse(Files.exists(cleanPath));

        WorktreeLease dirty = manager.createAgentWorktree("general-purpose");
        Files.writeString(dirty.workdir().resolve("tracked.txt"), "changed\n");
        var report = dirty.closeSafely();
        assertFalse(report.removed());
        assertTrue(Files.isDirectory(dirty.workdir()));
        assertTrue(manager.remove(dirty.session().slug(), true).removed());
    }

    @Test void postCreationCopiesLocalConfigAndUsesWorktreeHooks() throws Exception {
        Files.writeString(repository.resolve("config.yaml"), "provider: test\n");
        var created = manager.create("configured");
        assertEquals("provider: test\n", Files.readString(created.session().worktreePath().resolve("config.yaml")));
        GitCommandResult hooks = runner.run(created.session().worktreePath(),
                List.of("config", "--worktree", "--get", "core.hooksPath"));
        assertEquals(0, hooks.exitCode(), hooks.output());
        assertTrue(hooks.output().contains("hooks"));
        manager.remove("configured", false);
    }

    @Test void postCreationCopiesConfiguredIgnoredFilesAndLinksDependenciesWithSafeFallback() throws Exception {
        manager.close();
        Files.writeString(repository.resolve(".gitignore"),
                "config.yaml\n.imiocode/\n.env.local\nnode_modules/\n");
        git(repository, "add", ".gitignore");
        git(repository, "commit", "-m", "ignore local files");
        Files.writeString(repository.resolve("config.yaml"), "provider: local\n");
        Files.writeString(repository.resolve(".env.local"), "LOCAL_ONLY=yes\n");
        Files.createDirectories(repository.resolve("node_modules"));
        Files.writeString(repository.resolve("node_modules").resolve("dependency.txt"), "shared\n");
        Files.createDirectories(repository.resolve(".imiocode"));
        Files.writeString(repository.resolve(".imiocode").resolve("mcp.local.yaml"), "servers: {}\n");
        new WorktreeSessionStore(repository).save(new WorktreeSession(
                "00000000-0000-0000-0000-000000000000", "sentinel", repository,
                repository.resolve("sentinel"), "worktree-sentinel", "main",
                runner.checked(repository, List.of("rev-parse", "HEAD"), "head"), Instant.EPOCH));
        Files.createDirectories(repository.resolve(".imiocode").resolve("tool-results"));

        WorktreeConfig config = new WorktreeConfig(Path.of(".imiocode", "worktrees"),
                Duration.ofSeconds(10), Duration.ofDays(7), Duration.ofHours(1),
                List.of("node_modules"), List.of(".env.local"), true);
        manager = new WorktreeManager(repository, config);
        var created = manager.create("setup");
        Path worktree = created.session().worktreePath();

        assertEquals("provider: local\n", Files.readString(worktree.resolve("config.yaml")));
        assertEquals("LOCAL_ONLY=yes\n", Files.readString(worktree.resolve(".env.local")));
        assertEquals("servers: {}\n",
                Files.readString(worktree.resolve(".imiocode").resolve("mcp.local.yaml")));
        assertFalse(Files.exists(worktree.resolve(".imiocode").resolve("worktree-session.json")));
        assertFalse(Files.exists(worktree.resolve(".imiocode").resolve("tool-results")));

        Path linked = worktree.resolve("node_modules");
        if (Files.exists(linked)) {
            assertTrue(Files.isSymbolicLink(linked));
            assertEquals("shared\n", Files.readString(linked.resolve("dependency.txt")));
        } else {
            assertTrue(created.warnings().stream().anyMatch(value -> value.contains("node_modules")));
        }
        manager.remove("setup", false);
    }

    @Test void staleCleanupSkipsActiveAndDirtyWorktreesAndRemovesSafeOrphans() throws Exception {
        manager.close();
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        WorktreeConfig config = new WorktreeConfig(Path.of(".imiocode", "worktrees"),
                Duration.ofSeconds(10), Duration.ofHours(1), Duration.ofDays(1),
                List.of(), List.of(), false);
        manager = new WorktreeManager(repository, config, Clock.fixed(now, ZoneOffset.UTC));

        var clean = manager.create("agent-clean").session();
        var dirty = manager.create("agent-dirty").session();
        Files.writeString(dirty.worktreePath().resolve("tracked.txt"), "dirty\n");
        var active = manager.create("agent-active").session();
        var locked = manager.create("agent-locked").session();
        manager.enter("agent-active");
        FileTime old = FileTime.from(now.minus(Duration.ofHours(2)));
        Files.setLastModifiedTime(clean.worktreePath(), old);
        Files.setLastModifiedTime(dirty.worktreePath(), old);
        Files.setLastModifiedTime(active.worktreePath(), old);
        Files.setLastModifiedTime(locked.worktreePath(), old);
        Path orphanLock = repository.resolve(".imiocode").resolve("worktree-locks")
                .resolve("agent-orphan.lock");
        Files.writeString(orphanLock, "");
        Path liveLock = repository.resolve(".imiocode").resolve("worktree-locks")
                .resolve("agent-locked.lock");

        List<io.imiocode.worktree.model.WorktreeCleanupReport> reports;
        try (FileChannel channel = FileChannel.open(liveLock,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {
            reports = manager.cleanupStaleNow();
        }

        assertFalse(Files.exists(clean.worktreePath()));
        assertTrue(Files.isDirectory(dirty.worktreePath()));
        assertTrue(Files.isDirectory(active.worktreePath()));
        assertTrue(Files.isDirectory(locked.worktreePath()));
        assertFalse(Files.exists(orphanLock));
        assertTrue(reports.stream().anyMatch(report -> report.removed()
                && report.path().equals(clean.worktreePath())));
        assertTrue(reports.stream().anyMatch(report -> !report.removed()
                && report.path().equals(dirty.worktreePath())));

        manager.exit(false, false);
        manager.remove("agent-active", false);
        manager.remove("agent-locked", false);
        manager.remove("agent-dirty", true);
    }

    private void git(Path cwd, String... args) {
        GitCommandResult result = runner.run(cwd, List.of(args));
        assertEquals(0, result.exitCode(), result.output());
    }
}
