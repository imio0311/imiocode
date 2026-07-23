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

class WriteFileToolTest {
    @TempDir
    Path workspace;

    @Test
    void createsAndOverwritesUtf8File() throws Exception {
        WriteFileTool tool = tool();

        assertTrue(tool.execute(args("新文件.txt", "第一版")).success());
        assertTrue(tool.execute(args("新文件.txt", "第二版")).success());
        assertEquals("第二版", Files.readString(workspace.resolve("新文件.txt")));
    }

    @Test
    void leavesExistingFileUntouchedOnInvalidTarget() throws Exception {
        Files.writeString(workspace.resolve("keep.txt"), "保留");

        assertFalse(tool().execute(args("../keep.txt", "覆盖")).success());
        assertFalse(tool().execute(args("missing/file.txt", "内容")).success());
        assertEquals("保留", Files.readString(workspace.resolve("keep.txt")));
    }

    @Test
    void rejectsContentOverOneMibBeforeChangingTarget() throws Exception {
        Path target = workspace.resolve("keep.txt");
        Files.writeString(target, "保留");

        assertFalse(tool().execute(args("keep.txt", "x".repeat(1024 * 1024 + 1))).success());
        assertEquals("保留", Files.readString(target));
    }

    private WriteFileTool tool() {
        return new WriteFileTool(new WorkspacePolicy(workspace), ToolLimits.defaults(), new SecretRedactor(""));
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode args(String path, String content) {
        return JsonNodeFactory.instance.objectNode().put("path", path).put("content", content);
    }
}
