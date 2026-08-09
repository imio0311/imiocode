package io.imiocode.subagent.definition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AgentDefinitionLoaderTest {
    @TempDir Path temp;

    @Test void loadsThreeBuiltinsThroughSameParser() {
        AgentDefinitionLoader loader = new AgentDefinitionLoader(temp.resolve("project"), temp.resolve("user"));
        assertEquals(3, loader.snapshot().definitions().size());
        assertEquals("haiku", loader.require("explore").model().orElseThrow());
        assertEquals(15, loader.require("plan").maxTurns());
        assertTrue(loader.require("general-purpose").unrestrictedTools());
    }

    @Test void projectOverridesUserAndInvalidProjectFallsBack() throws Exception {
        Path workspace=temp.resolve("project"); Path user=temp.resolve("user");
        Files.createDirectories(workspace.resolve(".imiocode/agents"));
        Files.createDirectories(user.resolve(".imiocode/agents"));
        Files.writeString(user.resolve(".imiocode/agents/custom.md"), valid("user description"));
        Files.writeString(workspace.resolve(".imiocode/agents/custom.md"), valid("project description"));
        AgentDefinitionLoader loader=new AgentDefinitionLoader(workspace,user);
        assertEquals("project description",loader.require("custom").description());
        Files.writeString(workspace.resolve(".imiocode/agents/custom.md"),"not frontmatter");
        loader.reload();
        assertEquals("user description",loader.require("custom").description());
        assertFalse(loader.snapshot().diagnostics().isEmpty());
    }

    @Test void rejectsConflictingToolSets() {
        String markdown="""
                ---
                name: broken
                description: broken
                tools: [grep]
                disallowedTools: [grep]
                ---
                prompt
                """;
        assertThrows(AgentDefinitionException.class,()->new AgentDefinitionParser()
                .parse(markdown,AgentDefinitionSource.PROJECT,null));
    }

    private static String valid(String description) {
        return "---\nname: custom\ndescription: "+description+"\npermissionMode: read-only\n---\nprompt\n";
    }
}
