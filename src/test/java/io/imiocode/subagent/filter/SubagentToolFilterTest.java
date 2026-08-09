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
}
