package io.imiocode.tool.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspacePolicyTest {
    @TempDir
    Path workspace;

    @Test
    void resolvesOrdinaryAndChineseRelativeFiles() throws IOException {
        Path file = workspace.resolve("目录/文件.txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "内容");
        WorkspacePolicy policy = new WorkspacePolicy(workspace);

        assertEquals(file, policy.resolveExistingFile("目录/文件.txt"));
        assertTrue(policy.isAllowedDiscoveredPath(file));
    }

    @Test
    void rejectsAbsoluteTraversalAndProtectedPaths() throws IOException {
        Files.createDirectory(workspace.resolve(".git"));
        Files.writeString(workspace.resolve("config.yaml"), "secret");
        Files.writeString(workspace.resolve(".env.local"), "secret");
        WorkspacePolicy policy = new WorkspacePolicy(workspace);

        assertThrows(IllegalArgumentException.class, () -> policy.resolveExistingFile(""));
        assertThrows(IllegalArgumentException.class,
                () -> policy.resolveExistingFile(workspace.resolve("x").toString()));
        assertThrows(IllegalArgumentException.class, () -> policy.resolveWritableFile("../x"));
        assertThrows(IllegalArgumentException.class, () -> policy.resolveExistingPath(".git"));
        assertThrows(IllegalArgumentException.class, () -> policy.resolveExistingFile("config.yaml"));
        assertThrows(IllegalArgumentException.class, () -> policy.resolveExistingFile(".env.local"));
        assertFalse(policy.isAllowedDiscoveredPath(workspace.resolve(".git")));
    }

    @Test
    void rejectsSymbolicLinksWhenSupported() throws IOException {
        Path outside = Files.createTempDirectory("imiocode-outside-");
        Path link = workspace.resolve("link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (IOException | UnsupportedOperationException exception) {
            return;
        }
        WorkspacePolicy policy = new WorkspacePolicy(workspace);
        assertThrows(IllegalArgumentException.class, () -> policy.resolveExistingPath("link"));
        assertFalse(policy.isAllowedDiscoveredPath(link));
    }
}
