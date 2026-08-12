package io.imiocode.subagent.runtime;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLifecycleListener;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;
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
import java.util.List;
import java.util.Set;

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

    @Test void teamMemberRegistryPhysicallyDropsToolsOutsideSelection() throws Exception {
        Path parentDir=temp.resolve("parent");Path memberDir=temp.resolve("member");
        Files.createDirectories(parentDir);Files.createDirectories(memberDir);
        ToolLimits limits=ToolLimits.defaults();SecretRedactor redactor=new SecretRedactor("test-secret");
        WorkspacePolicy policy=new WorkspacePolicy(parentDir);ToolRegistry parent=new ToolRegistry();
        parent.register(new ReadFileTool(policy,limits,redactor));parent.register(new WriteFileTool(policy,limits,redactor));
        parent.register(new EditFileTool(policy,limits,redactor));parent.register(new BashTool(policy,limits,redactor));
        parent.register(new GlobTool(policy,limits,redactor));parent.register(new GrepTool(policy,limits,redactor));
        parent.register(tool("TeamCreate"));parent.register(tool("mcp_demo__query"));
        Tool teamTask=tool("TaskList");ToolRegistry member=new SubagentToolRegistryFactory(parent,limits,redactor)
                .createTeamMember(memberDir,ToolLifecycleListener.NOOP,List.of(teamTask),
                        ToolSelection.only(Set.of("read_file","TaskList")));

        assertEquals(Set.of("read_file","TaskList"),member.registeredNames());
        assertFalse(member.findEnabled("TeamCreate").isPresent());
        assertFalse(member.findEnabled("mcp_demo__query").isPresent());
    }

    private static void write(ToolRegistry registry, String content) {
        var args = JsonNodeFactory.instance.objectNode().put("path", "same.txt").put("content", content);
        var result = registry.findEnabled("write_file").orElseThrow().execute(args);
        assertTrue(result.success(), result.error());
    }

    private static Tool tool(String name){return new Tool(){
        private final ToolDefinition definition=new ToolDefinition(name,name,
                JsonNodeFactory.instance.objectNode(),ToolRisk.LOW);
        @Override public ToolDefinition definition(){return definition;}
        @Override public ToolResult execute(com.fasterxml.jackson.databind.node.ObjectNode arguments){
            return ToolResult.success("ok");
        }
    };}
}
