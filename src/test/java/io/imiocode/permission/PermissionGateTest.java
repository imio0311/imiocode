package io.imiocode.permission;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.permission.command.RegexDangerousCommandDetector;
import io.imiocode.permission.rule.PermissionRuleEngine;
import io.imiocode.permission.sandbox.PathSandbox;
import io.imiocode.permission.sandbox.SandboxResult;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionGateTest {
    @TempDir
    Path workspace;

    @Test
    void turnsAskIntoUserDecision() {
        PermissionCoordinator coordinator = new PermissionCoordinator();
        PermissionGate gate = gate(coordinator);
        ToolCall call = new ToolCall("1", "bash",
                JsonNodeFactory.instance.objectNode().put("command", "mvn test"));
        PermissionEvaluation evaluation = gate.evaluate(
                call,
                new ToolDefinition("bash", "bash",
                        JsonNodeFactory.instance.objectNode(), ToolRisk.HIGH));

        PermissionDecision decision = gate.confirm(
                evaluation,
                1,
                prompt -> assertTrue(gate.resolve(
                        prompt.requestId(), PermissionReply.ALLOW_ONCE)),
                (id, reply) -> { });

        assertEquals(PermissionAction.ALLOW, decision.action());
        gate.close();
    }

    private PermissionGate gate(PermissionCoordinator coordinator) {
        PathSandbox sandbox = new PathSandbox() {
            @Override
            public SandboxResult inspect(PermissionRequest request) {
                return SandboxResult.allow();
            }

            @Override
            public Path revalidateWritable(Path target) {
                return target;
            }
        };
        PermissionChecker checker = new PermissionChecker(
                workspace,
                new RegexDangerousCommandDetector(),
                sandbox,
                new PermissionRuleEngine(),
                new PermissionModePolicy(),
                PermissionSettings.defaults());
        return new PermissionGate(
                new PermissionRequestFactory(new SecretRedactor("")),
                checker,
                coordinator);
    }
}
