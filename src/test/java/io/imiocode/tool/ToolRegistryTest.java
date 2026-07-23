package io.imiocode.tool;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

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
