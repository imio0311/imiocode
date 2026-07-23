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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobToolTest {
    @TempDir
    Path workspace;

    @Test
    void matchesDoubleStarAndReturnsSortedUnixPaths() throws Exception {
        Files.createDirectories(workspace.resolve("src/子目录"));
        Files.writeString(workspace.resolve("z.java"), "");
        Files.writeString(workspace.resolve("src/B.java"), "");
        Files.writeString(workspace.resolve("src/子目录/A.java"), "");
        Files.writeString(workspace.resolve("src/no.txt"), "");

        ToolResult result = tool().execute(JsonNodeFactory.instance.objectNode().put("pattern", "**/*.java"));

        assertTrue(result.success());
        assertEquals("src/B.java\nsrc/子目录/A.java\nz.java", result.output());
    }

    @Test
    void omitsProtectedPaths() throws Exception {
        Files.createDirectory(workspace.resolve(".git"));
        Files.writeString(workspace.resolve(".git/secret.txt"), "");
        assertFalse(tool().execute(JsonNodeFactory.instance.objectNode().put("pattern", "**/*")).output()
                .contains("secret"));
    }

    @Test
    void supportsStarQuestionAndResultLimit() throws Exception {
        Files.writeString(workspace.resolve("a1.txt"), "");
        Files.writeString(workspace.resolve("a2.txt"), "");
        Files.writeString(workspace.resolve("b1.txt"), "");
        ToolLimits limits = new ToolLimits(
                1024, 100, 1024, 100, 1, 100, 100,
                4096, 1024, 1024, Duration.ofSeconds(1), Duration.ofMillis(100));
        GlobTool limited = new GlobTool(
                new WorkspacePolicy(workspace), limits, new SecretRedactor(""));

        ToolResult result = limited.execute(
                JsonNodeFactory.instance.objectNode().put("pattern", "a?.txt"));

        assertEquals("a1.txt\n...[输出已截断]", result.output());
        assertTrue(result.truncated());
    }

    private GlobTool tool() {
        return new GlobTool(new WorkspacePolicy(workspace), ToolLimits.defaults(), new SecretRedactor(""));
    }
}
