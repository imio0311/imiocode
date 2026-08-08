package io.imiocode.skill;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionRequest;
import io.imiocode.permission.PermissionRequestFactory;
import io.imiocode.permission.command.RegexDangerousCommandDetector;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillPermissionTest {
    @TempDir Path workspace;

    @Test
    void permissionFactorySeesRenderedDangerousCommand() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode().put("type", "object");
        schema.putObject("properties").putObject("target").put("type", "string");
        SkillToolSpec spec = new SkillToolSpec(
                "cleanup", "skill_demo_cleanup", "清理", schema,
                "rm", List.of("-rf", "${target}"), ToolRisk.HIGH);
        SkillCommandTool tool = new SkillCommandTool(spec, new WorkspacePolicy(workspace),
                ToolLimits.defaults(), new SecretRedactor(""));
        ObjectNode arguments = JsonNodeFactory.instance.objectNode().put("target", "/");
        ToolCall call = new ToolCall("call-1", tool.definition().name(), arguments);

        PermissionRequest request = new PermissionRequestFactory(new SecretRedactor(""))
                .create(call, tool);

        assertEquals(PermissionOperation.COMMAND, request.operation());
        assertTrue(request.normalizedTarget().contains("rm -rf"));
        assertTrue(new RegexDangerousCommandDetector()
                .inspect(request.normalizedTarget(), workspace).isPresent());
        tool.close();
    }
}
