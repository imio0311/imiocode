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
import java.time.Duration;

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

    @Test
    void limitsMatchesAndLongLinesAndSkipsProtectedContent() throws Exception {
        Files.createDirectory(workspace.resolve(".git"));
        Files.writeString(workspace.resolve(".git/secret"), "UNIQUE_SECRET");
        Files.writeString(workspace.resolve("a.txt"), "match-" + "x".repeat(100) + "\nmatch-two\n");
        ToolLimits limits = new ToolLimits(
                1024, 100, 1024, 100, 100, 1, 12,
                4096, 1024, 1024, Duration.ofSeconds(1), Duration.ofMillis(100));
        GrepTool limited = new GrepTool(
                new WorkspacePolicy(workspace), limits, new SecretRedactor(""));

        ToolResult result = limited.execute(
                JsonNodeFactory.instance.objectNode().put("pattern", "match|UNIQUE_SECRET"));

        assertTrue(result.truncated());
        assertTrue(result.output().contains("a.txt:1:match-xxxxxx..."));
        assertFalse(result.output().contains("UNIQUE_SECRET"));
    }

    private GrepTool tool() {
        return new GrepTool(new WorkspacePolicy(workspace), ToolLimits.defaults(), new SecretRedactor(""));
    }
}
