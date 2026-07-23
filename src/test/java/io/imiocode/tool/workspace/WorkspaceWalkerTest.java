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
}
