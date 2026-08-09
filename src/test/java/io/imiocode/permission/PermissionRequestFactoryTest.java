package io.imiocode.permission;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRisk;
import io.imiocode.permission.command.CommandRiskAssessment;
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

    @Test
    void classifiesMcpToolsAsCommandsWithoutCopyingArguments() {
        PermissionRequest request = factory.create(
                call("mcp_github__issues_list", "token", "secret-key"),
                definition("mcp_github__issues_list", ToolRisk.HIGH));

        assertEquals(PermissionOperation.COMMAND, request.operation());
        assertEquals("mcp_github__issues_list", request.normalizedTarget());
        assertEquals("mcp_github__issues_list", request.displayTarget());
        assertTrue(!request.displayTarget().contains("secret-key"));
    }

    @Test
    void usesDynamicRiskForBashButKeepsMcpStaticRisk() {
        PermissionRequestFactory dynamic = new PermissionRequestFactory(
                new SecretRedactor("secret-key"),
                command -> command.startsWith("git push")
                        ? new CommandRiskAssessment(ToolRisk.HIGH, "Git 远程写入")
                        : new CommandRiskAssessment(ToolRisk.MEDIUM, "普通本地开发命令"));

        PermissionRequest build = dynamic.create(
                call("bash", "command", "mvn test"), definition("bash", ToolRisk.HIGH));
        PermissionRequest push = dynamic.create(
                call("bash", "command", "git push origin main"), definition("bash", ToolRisk.HIGH));
        PermissionRequest mcp = dynamic.create(
                call("mcp_demo__query", "value", "x"), definition("mcp_demo__query", ToolRisk.HIGH));

        assertEquals(ToolRisk.MEDIUM, build.risk());
        assertEquals("普通本地开发命令", build.riskReason());
        assertEquals(ToolRisk.HIGH, push.risk());
        assertEquals(ToolRisk.HIGH, mcp.risk());
    }

    private static ToolCall call(String tool, String field, String value) {
        return new ToolCall("1", tool, JsonNodeFactory.instance.objectNode().put(field, value));
    }

    private static ToolDefinition definition(String name, ToolRisk risk) {
        return new ToolDefinition(name, name, JsonNodeFactory.instance.objectNode(), risk);
    }
}
