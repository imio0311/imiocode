package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadFileToolTest {
    @TempDir
    Path workspace;

    @Test
    void readsLineRangeWithNumbersAndUtf8() throws Exception {
        Files.writeString(workspace.resolve("中文.txt"), "甲\n乙\n丙\n");
        ReadFileTool tool = tool();
        ObjectNode arguments = JsonNodeFactory.instance.objectNode()
                .put("path", "中文.txt").put("start_line", 2).put("end_line", 3);

        ToolResult result = tool.execute(arguments);

        assertTrue(result.success());
        assertTrue(result.output().contains("2: 乙"));
        assertTrue(result.output().contains("3: 丙"));
        assertFalse(result.output().contains("1:"));
    }

    @Test
    void rejectsBinaryInvalidUtf8AndProtectedFiles() throws Exception {
        Files.write(workspace.resolve("binary.bin"), new byte[]{65, 0, 66});
        Files.write(workspace.resolve("invalid.txt"), new byte[]{(byte) 0xC3, 0x28});
        Files.writeString(workspace.resolve("config.yaml"), "secret");

        assertFalse(tool().execute(args("binary.bin")).success());
        assertFalse(tool().execute(args("invalid.txt")).success());
        assertFalse(tool().execute(args("config.yaml")).success());
    }

    private ReadFileTool tool() {
        return new ReadFileTool(new WorkspacePolicy(workspace), ToolLimits.defaults(), new SecretRedactor(""));
    }

    private static ObjectNode args(String path) {
        return JsonNodeFactory.instance.objectNode().put("path", path);
    }
}
