package io.imiocode.tool.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtomicFileWriterTest {
    @TempDir
    Path workspace;

    @Test
    void createsAndReplacesUtf8FileWithoutTemporaryResidue() throws IOException {
        AtomicFileWriter writer = new AtomicFileWriter(new WorkspacePolicy(workspace));

        writer.write("内容.txt", "第一版");
        writer.write("内容.txt", "第二版");

        assertEquals("第二版", Files.readString(workspace.resolve("内容.txt")));
        try (var paths = Files.list(workspace)) {
            assertEquals(1, paths.count());
        }
    }

    @Test
    void failureDoesNotChangeExistingTarget() throws IOException {
        Path target = workspace.resolve("keep.txt");
        Files.writeString(target, "原内容");
        AtomicFileWriter writer = new AtomicFileWriter(new WorkspacePolicy(workspace));

        assertThrows(IllegalArgumentException.class, () -> writer.write("../keep.txt", "新内容"));
        assertEquals("原内容", Files.readString(target));
    }
}
