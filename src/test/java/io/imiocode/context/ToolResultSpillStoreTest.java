package io.imiocode.context;

import io.imiocode.conversation.ToolResultPart;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolResultSpillStoreTest {
    @TempDir Path workspace;

    @Test
    void namesAndEncodesResultsIdempotently() throws Exception {
        SecretRedactor redactor = new SecretRedactor("secret-key");
        ToolResultSpillStore store = new ToolResultSpillStore(workspace, redactor);
        ToolResultPart part = new ToolResultPart("../CON\\坏", "bash",
                ToolResult.failure("输出😀secret-key", "错误", true, 7));

        SpilledResult first = store.spill(part);
        SpilledResult second = store.spill(part);

        assertEquals(first.relativePath(), second.relativePath());
        assertTrue(first.newlyCreated());
        assertFalse(second.newlyCreated());
        Path target = workspace.resolve(first.relativePath());
        assertTrue(target.normalize().startsWith(workspace.resolve(".imiocode/tool-results")));
        String body = Files.readString(target, StandardCharsets.UTF_8);
        assertTrue(body.contains("输出😀***"));
        assertTrue(body.contains("success: false"));
        assertFalse(body.contains("secret-key"));
    }

    @Test
    void differentContentWithSameIdNeverOverwrites() throws Exception {
        ToolResultSpillStore store = new ToolResultSpillStore(workspace, new SecretRedactor(""));
        SpilledResult first = store.spill(part("same", "a"));
        SpilledResult second = store.spill(part("same", "b"));
        assertFalse(first.relativePath().equals(second.relativePath()));
        assertEquals(2, Files.list(workspace.resolve(".imiocode/tool-results")).count());
    }

    @Test
    void unsafeMetadataDirectoryFailsWithoutWritingElsewhere() throws Exception {
        Files.writeString(workspace.resolve(".imiocode"), "not a directory");
        ToolResultSpillStore store = new ToolResultSpillStore(workspace, new SecretRedactor(""));
        assertThrows(Exception.class, () -> store.spill(part("id", "content")));
    }

    private static ToolResultPart part(String id, String output) {
        return new ToolResultPart(id, "read_file", ToolResult.success(output));
    }
}
