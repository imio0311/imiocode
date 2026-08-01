package io.imiocode.tool.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceWalkerTest {
    @TempDir
    Path workspace;

    @Test
    void walksInStableOrderAndSkipsProtectedPaths() throws Exception {
        Files.writeString(workspace.resolve("z.txt"), "z");
        Files.createDirectory(workspace.resolve("a"));
        Files.writeString(workspace.resolve("a/b.txt"), "b");
        Files.createDirectory(workspace.resolve(".git"));
        Files.writeString(workspace.resolve(".git/secret"), "x");
        WorkspacePolicy policy = new WorkspacePolicy(workspace);

        WorkspaceWalker.WalkResult result =
                new WorkspaceWalker(policy).walk(workspace, 100, () -> false);
        List<String> paths = result.paths().stream().map(policy::relativeUnixPath).toList();

        assertEquals(List.of("a", "z.txt", "a/b.txt"), paths);
        assertFalse(result.truncated());
    }

    @Test
    void stopsAtScanLimit() throws Exception {
        Files.writeString(workspace.resolve("a"), "a");
        Files.writeString(workspace.resolve("b"), "b");

        WorkspaceWalker.WalkResult result = new WorkspaceWalker(new WorkspacePolicy(workspace))
                .walk(workspace, 1, () -> false);

        assertEquals(1, result.scannedPaths());
        assertEquals(1, result.paths().size());
        assertTrue(result.truncated());
    }

    @Test
    void skipsOnlyInternalToolResultsDirectory() throws Exception {
        Files.createDirectories(workspace.resolve(".imiocode/tool-results"));
        Files.writeString(workspace.resolve(".imiocode/tool-results/large.txt"), "secret result");
        Files.createDirectory(workspace.resolve("tool-results"));
        Files.writeString(workspace.resolve("tool-results/normal.txt"), "normal");
        WorkspacePolicy policy = new WorkspacePolicy(workspace);

        List<String> paths = new WorkspaceWalker(policy).walk(workspace, 100, () -> false)
                .paths().stream().map(policy::relativeUnixPath).toList();

        assertFalse(paths.stream().anyMatch(path -> path.startsWith(".imiocode/tool-results")));
        assertTrue(paths.contains("tool-results/normal.txt"));
        assertTrue(Files.readString(policy.resolveExistingFile(".imiocode/tool-results/large.txt"))
                .contains("secret result"));
    }
}
