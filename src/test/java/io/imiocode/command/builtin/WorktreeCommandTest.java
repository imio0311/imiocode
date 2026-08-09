package io.imiocode.command.builtin;

import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandOutcome;
import io.imiocode.command.CommandRegistry;
import io.imiocode.command.CommandServices;
import io.imiocode.command.CommandStatus;
import io.imiocode.command.ConfirmationPrompt;
import io.imiocode.command.UIController;
import io.imiocode.config.UiVerbosity;
import io.imiocode.worktree.config.WorktreeConfig;
import io.imiocode.worktree.git.GitCommandResult;
import io.imiocode.worktree.git.GitCommandRunner;
import io.imiocode.worktree.lifecycle.WorktreeManager;
import io.imiocode.worktree.runtime.WorkspaceTransition;
import io.imiocode.worktree.runtime.WorkspaceTransitionController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorktreeCommandTest {
    @TempDir Path temp;
    private Path repository;
    private WorktreeManager manager;
    private TestUi ui;
    private CommandContext context;

    @BeforeEach
    void setUp() throws Exception {
        repository = temp.resolve("repo");
        Files.createDirectories(repository);
        GitCommandRunner git = new GitCommandRunner(Duration.ofSeconds(10));
        runGit(git, repository, "init");
        runGit(git, repository, "config", "user.email", "test@example.invalid");
        runGit(git, repository, "config", "user.name", "Test");
        Files.writeString(repository.resolve("tracked.txt"), "root\n");
        Files.writeString(repository.resolve(".gitignore"), ".imiocode/\n");
        runGit(git, repository, "add", ".");
        runGit(git, repository, "commit", "-m", "initial");
        manager = new WorktreeManager(repository, WorktreeConfig.defaults());
        ui = new TestUi();
        CommandServices unused = (CommandServices) Proxy.newProxyInstance(
                CommandServices.class.getClassLoader(), new Class<?>[]{CommandServices.class},
                (proxy, method, args) -> { throw new AssertionError("worktree 命令不应调用 Agent 服务: " + method.getName()); });
        context = new CommandContext(unused, ui, new CommandRegistry());
    }

    @AfterEach
    void tearDown() {
        if (manager != null) manager.close();
    }

    @Test
    void supportsCreateListEnterExitAndConfirmedRemoveLocally() {
        WorkspaceTransitionController enterTransition = new WorkspaceTransitionController();
        WorktreeCommand command = new WorktreeCommand(manager, enterTransition);

        var created = command.execute(context, List.of("create", "demo"));
        assertEquals(CommandOutcome.HANDLED, created.outcome());
        assertTrue(created.messages().getFirst().text().contains("demo"));

        var listed = command.execute(context, List.of("list"));
        assertEquals(CommandOutcome.HANDLED, listed.outcome());
        assertTrue(listed.messages().getFirst().text().contains("worktree-demo"));

        var entered = command.execute(context, List.of("enter", "demo"));
        assertEquals(CommandOutcome.RESTART_REQUESTED, entered.outcome());
        WorkspaceTransition.Enter enter = assertInstanceOf(
                WorkspaceTransition.Enter.class, enterTransition.current());
        assertEquals(manager.list().getFirst().path(), enter.path());
        assertTrue(manager.hasRecoverableSession());

        WorkspaceTransitionController exitTransition = new WorkspaceTransitionController();
        var exited = new WorktreeCommand(manager, exitTransition)
                .execute(context, List.of("exit", "keep"));
        assertEquals(CommandOutcome.RESTART_REQUESTED, exited.outcome());
        WorkspaceTransition.Exit exit = assertInstanceOf(
                WorkspaceTransition.Exit.class, exitTransition.current());
        assertEquals(repository.toAbsolutePath().normalize(), exit.path());
        assertFalse(manager.hasRecoverableSession());
        assertEquals(1, manager.list().size());

        ui.confirmation = false;
        var cancelled = command.execute(context, List.of("remove", "demo"));
        assertEquals(CommandOutcome.HANDLED, cancelled.outcome());
        assertEquals(1, manager.list().size());

        ui.confirmation = true;
        var removed = command.execute(context, List.of("remove", "demo"));
        assertEquals(CommandOutcome.HANDLED, removed.outcome());
        assertTrue(manager.list().isEmpty());
        assertEquals(2, ui.confirmCalls);
    }

    @Test
    void exitRemoveRequiresConfirmationBeforeDiscardingDirtyWorktree() throws Exception {
        WorktreeCommand enter = new WorktreeCommand(manager, new WorkspaceTransitionController());
        enter.execute(context, List.of("create", "dirty"));
        enter.execute(context, List.of("enter", "dirty"));
        Path worktree = manager.list().getFirst().path();
        Files.writeString(worktree.resolve("tracked.txt"), "dirty\n");

        ui.confirmation = false;
        var cancelled = new WorktreeCommand(manager, new WorkspaceTransitionController())
                .execute(context, List.of("exit", "remove"));
        assertEquals(CommandOutcome.HANDLED, cancelled.outcome());
        assertTrue(Files.isDirectory(worktree));
        assertTrue(manager.hasRecoverableSession());

        ui.confirmation = true;
        WorkspaceTransitionController transition = new WorkspaceTransitionController();
        var removed = new WorktreeCommand(manager, transition)
                .execute(context, List.of("exit", "remove"));
        assertEquals(CommandOutcome.RESTART_REQUESTED, removed.outcome());
        assertFalse(Files.exists(worktree));
        assertFalse(manager.hasRecoverableSession());
        assertInstanceOf(WorkspaceTransition.Exit.class, transition.current());
    }

    private static void runGit(GitCommandRunner git, Path cwd, String... args) {
        GitCommandResult result = git.run(cwd, List.of(args));
        assertEquals(0, result.exitCode(), result.output());
    }

    private static final class TestUi implements UIController {
        private boolean confirmation;
        private int confirmCalls;
        @Override public void clearScreen() { }
        @Override public boolean confirm(ConfirmationPrompt prompt) { confirmCalls++; return confirmation; }
        @Override public UiVerbosity verbosity() { return UiVerbosity.COMPACT; }
        @Override public void setVerbosity(UiVerbosity verbosity) { }
        @Override public void refreshStatus(CommandStatus status) { }
    }
}
