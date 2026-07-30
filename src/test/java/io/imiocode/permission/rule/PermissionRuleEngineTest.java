package io.imiocode.permission.rule;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.permission.PermissionAction;
import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionRequest;
import io.imiocode.permission.PermissionRule;
import io.imiocode.permission.PermissionRuleLayer;
import io.imiocode.permission.PermissionSettings;
import io.imiocode.permission.PermissionMode;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionRuleEngineTest {
    @Test
    void supportsGlobAndLayerPriority() {
        PermissionRule user = rule(
                PermissionRuleLayer.USER, PermissionAction.DENY, "read_*", "src/**");
        PermissionRule project = rule(
                PermissionRuleLayer.PROJECT, PermissionAction.ALLOW, "*", "**");
        PermissionSettings settings = new PermissionSettings(
                PermissionMode.FULL_ACCESS, List.of(user), List.of(project), List.of());

        var decision = new PermissionRuleEngine().evaluate(
                request("read_file", "src/Main.java"), settings).orElseThrow();

        assertEquals(PermissionAction.DENY, decision.action());
    }

    @Test
    void firstRuleWinsAndMissingTargetMatchesAll() {
        PermissionRule first = rule(
                PermissionRuleLayer.PROJECT, PermissionAction.ASK, "bash", null);
        PermissionRule second = rule(
                PermissionRuleLayer.PROJECT, PermissionAction.ALLOW, "bash", "mvn *");
        PermissionSettings settings = new PermissionSettings(
                PermissionMode.ASK, List.of(), List.of(first, second), List.of());

        assertEquals(PermissionAction.ASK, new PermissionRuleEngine()
                .evaluate(request("bash", "mvn test"), settings)
                .orElseThrow().action());
        assertFalse(new PermissionGlobMatcher().matches("src/*", "src/a/b.java"));
        assertTrue(new PermissionGlobMatcher().matches("src/**", "src/a/b.java"));
    }

    private static PermissionRule rule(
            PermissionRuleLayer layer,
            PermissionAction action,
            String tool,
            String target
    ) {
        return new PermissionRule(layer, action, tool, Optional.ofNullable(target));
    }

    private static PermissionRequest request(String tool, String target) {
        PermissionOperation operation = "bash".equals(tool)
                ? PermissionOperation.COMMAND : PermissionOperation.READ;
        return new PermissionRequest(
                new ToolCall("1", tool, JsonNodeFactory.instance.objectNode()),
                "bash".equals(tool) ? ToolRisk.HIGH : ToolRisk.LOW,
                operation,
                target,
                target);
    }
}
