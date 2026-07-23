package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditFileToolTest {
    @TempDir
    Path workspace;

    @Test
    void replacesExactlyOneOccurrenceIncludingWithEmptyText() throws Exception {
        Files.writeString(workspace.resolve("code.txt"), "你好 old 世界");

        assertTrue(tool().execute(args("old", "")).success());
        assertEquals("你好  世界", Files.readString(workspace.resolve("code.txt")));
    }

    @Test
    void zeroOrMultipleMatchesLeaveBytesUnchanged() throws Exception {
        Path file = workspace.resolve("code.txt");
        Files.writeString(file, "same same");
        byte[] before = Files.readAllBytes(file);

        assertFalse(tool().execute(args("missing", "x")).success());
        assertFalse(tool().execute(args("same", "x")).success());
        assertEquals(java.util.HexFormat.of().formatHex(before),
                java.util.HexFormat.of().formatHex(Files.readAllBytes(file)));
    }

    private EditFileTool tool() {
        return new EditFileTool(new WorkspacePolicy(workspace), ToolLimits.defaults(), new SecretRedactor(""));
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode args(String oldText, String newText) {
        return JsonNodeFactory.instance.objectNode()
                .put("path", "code.txt").put("old_text", oldText).put("new_text", newText);
    }
}
