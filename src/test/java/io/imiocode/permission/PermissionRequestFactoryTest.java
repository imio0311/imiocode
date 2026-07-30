package io.imiocode.permission;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionRequestFactoryTest {
    private final PermissionRequestFactory factory =
            new PermissionRequestFactory(new SecretRedactor("secret-key"));

    @Test
    void extractsAndNormalizesCoreToolTargets() {
        PermissionRequest file = factory.create(
                call("read_file", "path", "src\\Main.java"),
                definition("read_file", ToolRisk.LOW));
        PermissionRequest grep = factory.create(
                new ToolCall("2", "grep", JsonNodeFactory.instance.objectNode()
                        .put("pattern", "Agent")),
                definition("grep", ToolRisk.LOW));
        PermissionRequest bash = factory.create(
                call("bash", "command", "  mvn   test  "),
                definition("bash", ToolRisk.HIGH));

        assertEquals("src/Main.java", file.normalizedTarget());
        assertEquals(".", grep.normalizedTarget());
        assertEquals("mvn test", bash.normalizedTarget());
        assertEquals(PermissionOperation.COMMAND, bash.operation());
    }

    @Test
    void redactsAndRejectsInvalidArguments() {
        PermissionRequest request = factory.create(
                call("bash", "command", "echo secret-key"),
                definition("bash", ToolRisk.HIGH));
        assertTrue(request.displayTarget().contains("***"));
        assertThrows(IllegalArgumentException.class, () -> factory.create(
                new ToolCall("3", "write_file", JsonNodeFactory.instance.objectNode()),
                definition("write_file", ToolRisk.MEDIUM)));
    }

    private static ToolCall call(String tool, String field, String value) {
        return new ToolCall("1", tool, JsonNodeFactory.instance.objectNode().put(field, value));
    }

    private static ToolDefinition definition(String name, ToolRisk risk) {
        return new ToolDefinition(name, name, JsonNodeFactory.instance.objectNode(), risk);
    }
}
