package io.imiocode.subagent.runtime;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLifecycleListener;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.core.BashTool;
import io.imiocode.tool.core.EditFileTool;
import io.imiocode.tool.core.GlobTool;
import io.imiocode.tool.core.GrepTool;
import io.imiocode.tool.core.ReadFileTool;
import io.imiocode.tool.core.WriteFileTool;
import io.imiocode.tool.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SubagentToolRegistryFactoryTest {
    @TempDir Path temp;

    @Test void sameRelativeWriteIsBoundToEachScopedWorkspace() throws Exception {
        Path parentDir = temp.resolve("parent"); Path firstDir = temp.resolve("first"); Path secondDir = temp.resolve("second");
        Files.createDirectories(parentDir); Files.createDirectories(firstDir); Files.createDirectories(secondDir);
        ToolLimits limits = ToolLimits.defaults(); SecretRedactor redactor = new SecretRedactor("test-secret");
        WorkspacePolicy parentPolicy = new WorkspacePolicy(parentDir); ToolRegistry parent = new ToolRegistry();
        parent.register(new ReadFileTool(parentPolicy, limits, redactor));
        parent.register(new WriteFileTool(parentPolicy, limits, redactor));
        parent.register(new EditFileTool(parentPolicy, limits, redactor));
        parent.register(new BashTool(parentPolicy, limits, redactor));
        parent.register(new GlobTool(parentPolicy, limits, redactor));
        parent.register(new GrepTool(parentPolicy, limits, redactor));
        SubagentToolRegistryFactory factory = new SubagentToolRegistryFactory(parent, limits, redactor);
        ToolRegistry first = factory.create(firstDir, ToolLifecycleListener.NOOP);
        ToolRegistry second = factory.create(secondDir, ToolLifecycleListener.NOOP);

        write(first, "one"); write(second, "two");
        assertEquals("one", Files.readString(firstDir.resolve("same.txt")));
        assertEquals("two", Files.readString(secondDir.resolve("same.txt")));
        assertFalse(Files.exists(parentDir.resolve("same.txt")));
    }

    private static void write(ToolRegistry registry, String content) {
        var args = JsonNodeFactory.instance.objectNode().put("path", "same.txt").put("content", content);
        var result = registry.findEnabled("write_file").orElseThrow().execute(args);
        assertTrue(result.success(), result.error());
    }
}
