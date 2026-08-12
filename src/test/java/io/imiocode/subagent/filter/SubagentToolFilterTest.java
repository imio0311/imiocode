package io.imiocode.subagent.filter;

import io.imiocode.subagent.config.SubagentConfig;
import io.imiocode.subagent.definition.AgentDefinitionParser;
import io.imiocode.subagent.definition.AgentDefinitionSource;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SubagentToolFilterTest {
    @Test void everyLayerOnlyNarrowsAndBackgroundIsReadOnly() {
        var definition=new AgentDefinitionParser().parse("""
                ---
                name: custom
                description: custom
                permissionMode: read-only
                tools: [read_file, grep, bash, write_file]
                disallowedTools: [edit_file]
                ---
                prompt
                """, AgentDefinitionSource.PROJECT,null);
        var config=new SubagentConfig(Map.of(),Set.of("write_file","grep"),Set.of("read_file","grep","bash"),2,16,16);
        definition=definition.withAdditionalDeniedTools(Set.of("read_file"));
        var selection=new SubagentToolFilter(config).select(definition,
                Set.of("read_file","grep","bash","write_file","edit_file"),true);
        assertEquals(Set.of(),selection.allowedNames());
        assertFalse(selection.allows("bash"));
    }

    @Test void ordinarySubagentNeverInheritsTeamControlPlane() {
        var definition=new AgentDefinitionParser().parse("""
                ---
                name: ordinary
                description: ordinary
                ---
                prompt
                """,AgentDefinitionSource.PROJECT,null);
        var selection=new SubagentToolFilter(SubagentConfig.defaults()).select(definition,
                Set.of("read_file","TeamCreate","TeamDelete","TeamConverge","TaskList","SendMessage"),false);
        assertEquals(Set.of("read_file"),selection.allowedNames());
    }

    @Test void teamMemberDefaultsToCoreWorkToolsAndRequiresExplicitExtensionAuthorization() {
        var unrestricted = new AgentDefinitionParser().parse("""
                ---
                name: worker
                description: worker
                ---
                prompt
                """, AgentDefinitionSource.PROJECT, null);
        var filter = new SubagentToolFilter(SubagentConfig.defaults());
        Set<String> enabled = Set.of("read_file", "write_file", "bash", "load_skill",
                "install_skill", "TeamCreate", "TeamConverge", "agent", "mcp_demo__query");

        var defaults = filter.selectTeamMember(unrestricted, enabled);
        assertEquals(Set.of("read_file", "write_file", "bash"), defaults.allowedNames());

        var explicit = new AgentDefinitionParser().parse("""
                ---
                name: mcp-worker
                description: worker
                tools: [read_file, mcp_demo__query, TeamCreate]
                ---
                prompt
                """, AgentDefinitionSource.PROJECT, null);
        var authorized = filter.selectTeamMember(explicit, enabled);
        assertEquals(Set.of("read_file", "mcp_demo__query"), authorized.allowedNames());
        assertFalse(authorized.allows("TeamCreate"));
    }
}
