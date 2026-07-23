package io.imiocode.tool;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import io.imiocode.tool.core.BashTool;
import io.imiocode.tool.core.EditFileTool;
import io.imiocode.tool.core.GlobTool;
import io.imiocode.tool.core.GrepTool;
import io.imiocode.tool.core.ReadFileTool;
import io.imiocode.tool.core.WriteFileTool;
import io.imiocode.tool.workspace.WorkspacePolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRegistryTest {
    @Test
    void registersEnabledToolsAndExportsStableOrder() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new StubTool("zeta"));
        registry.register(new StubTool("alpha"));

        assertEquals(List.of("alpha", "zeta"),
                registry.enabledDefinitions().stream().map(ToolDefinition::name).toList());
        assertEquals(List.of("api:alpha", "api:zeta"),
                registry.exportEnabled(definition -> "api:" + definition.name()));
    }

    @Test
    void disablesAndEnablesRegisteredTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new StubTool("alpha"));

        registry.disable("alpha");
        assertFalse(registry.findEnabled("alpha").isPresent());
        assertEquals(List.of(), registry.enabledDefinitions());

        registry.enable("alpha");
        assertTrue(registry.findEnabled("alpha").isPresent());
    }

    @Test
    void rejectsDuplicateAndUnknownNames() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new StubTool("alpha"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(new StubTool("alpha")));
        assertThrows(IllegalArgumentException.class, () -> registry.disable("missing"));
        assertFalse(registry.findEnabled("missing").isPresent());
    }

    @Test
    void registersAllSixCoreDefinitionsWithSchemasAndRisks(@TempDir Path workspace) {
        ToolRegistry registry = new ToolRegistry();
        ToolLimits limits = ToolLimits.defaults();
        SecretRedactor redactor = new SecretRedactor("");
        WorkspacePolicy policy = new WorkspacePolicy(workspace);
        registry.register(new ReadFileTool(policy, limits, redactor));
        registry.register(new WriteFileTool(policy, limits, redactor));
        registry.register(new EditFileTool(policy, limits, redactor));
        registry.register(new BashTool(policy, limits, redactor));
        registry.register(new GlobTool(policy, limits, redactor));
        registry.register(new GrepTool(policy, limits, redactor));

        List<ToolDefinition> definitions = registry.enabledDefinitions();
        assertEquals(List.of("bash", "edit_file", "glob", "grep", "read_file", "write_file"),
                definitions.stream().map(ToolDefinition::name).toList());
        definitions.forEach(definition -> {
            assertEquals("object", definition.inputSchema().path("type").asText());
            assertTrue(definition.inputSchema().path("required").isArray());
            assertFalse(definition.inputSchema().path("additionalProperties").asBoolean(true));
        });
        assertEquals(ToolRisk.HIGH, definitions.get(0).risk());
        assertEquals(ToolRisk.MEDIUM, definitions.get(1).risk());
        assertEquals(ToolRisk.MEDIUM, definitions.get(5).risk());
    }

    private static final class StubTool implements Tool {
        private final ToolDefinition definition;

        private StubTool(String name) {
            definition = new ToolDefinition(
                    name,
                    "测试工具",
                    JsonNodeFactory.instance.objectNode().put("type", "object"),
                    ToolRisk.LOW);
        }

        @Override
        public ToolDefinition definition() {
            return definition;
        }

        @Override
        public ToolResult execute(ObjectNode arguments) {
            return ToolResult.success("ok");
        }
    }
}
