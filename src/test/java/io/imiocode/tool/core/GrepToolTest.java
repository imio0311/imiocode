package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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

class GrepToolTest {
    @TempDir
    Path workspace;

    @Test
    void findsRegexWithRelativePathAndLineNumber() throws Exception {
        Files.createDirectory(workspace.resolve("src"));
        Files.writeString(workspace.resolve("src/A.java"), "第一行\nclass Agent {}\n");

        ToolResult result = tool().execute(JsonNodeFactory.instance.objectNode()
                .put("pattern", "class\\s+Agent").put("path", "src"));

        assertTrue(result.success());
        assertTrue(result.output().contains("src/A.java:2:class Agent {}"));
    }

    @Test
    void rejectsInvalidRegexAndSkipsBinary() throws Exception {
        Files.write(workspace.resolve("binary"), new byte[]{65, 0, 66});
        assertFalse(tool().execute(JsonNodeFactory.instance.objectNode().put("pattern", "[")).success());
        assertTrue(tool().execute(JsonNodeFactory.instance.objectNode().put("pattern", "A")).success());
    }

    private GrepTool tool() {
        return new GrepTool(new WorkspacePolicy(workspace), ToolLimits.defaults(), new SecretRedactor(""));
    }
}
