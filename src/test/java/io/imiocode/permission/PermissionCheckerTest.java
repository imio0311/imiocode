package io.imiocode.permission;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.permission.command.RegexDangerousCommandDetector;
import io.imiocode.permission.command.StrictSafeCommandDetector;
import io.imiocode.permission.rule.PermissionRuleEngine;
import io.imiocode.permission.sandbox.PathSandbox;
import io.imiocode.permission.sandbox.SandboxResult;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolRisk;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionCheckerTest {
    @TempDir
    Path workspace;

    @Test
    void hardBlockWinsOverFullAccessAndAllowRule() {
        PermissionRule allow = new PermissionRule(
                PermissionRuleLayer.USER,
                PermissionAction.ALLOW,
                "bash",
                Optional.of("**"));
        PermissionChecker checker = checker(new PermissionSettings(
                PermissionMode.FULL_ACCESS, List.of(allow), List.of(), List.of()));

        PermissionDecision decision = checker.check(command("git reset --hard"));

        assertEquals(PermissionAction.DENY, decision.action());
        assertEquals(PermissionDecisionSource.DANGEROUS_COMMAND, decision.source());
    }

    @Test
    void readOnlyCeilingWinsOverAllowRule() {
        PermissionRule allow = new PermissionRule(
                PermissionRuleLayer.USER,
                PermissionAction.ALLOW,
                "*",
                Optional.empty());
        PermissionChecker checker = checker(new PermissionSettings(
                PermissionMode.READ_ONLY, List.of(allow), List.of(), List.of()));

        assertEquals(PermissionAction.DENY,
                checker.check(command("mvn test")).action());
    }

    @Test
    void safeCommandSkipsHitlInAskAndAutoEditModes() {
        for (PermissionMode mode : List.of(PermissionMode.ASK, PermissionMode.AUTO_EDIT)) {
            PermissionChecker checker = strictChecker(new PermissionSettings(
                    mode, List.of(), List.of(), List.of()));

            PermissionDecision decision = checker.check(command("git status"));

            assertEquals(PermissionAction.ALLOW, decision.action());
            assertEquals(PermissionDecisionSource.SAFE_COMMAND, decision.source());
        }
    }

    @Test
    void explicitDenyAndAskRulesOverrideSafeCommand() {
        for (PermissionAction action : List.of(PermissionAction.DENY, PermissionAction.ASK)) {
            PermissionRule rule = new PermissionRule(
                    PermissionRuleLayer.USER,
                    action,
                    "bash",
                    Optional.of("git status"));
            PermissionChecker checker = strictChecker(new PermissionSettings(
                    PermissionMode.ASK, List.of(rule), List.of(), List.of()));

            PermissionDecision decision = checker.check(command("git status"));

            assertEquals(action, decision.action());
            assertEquals(PermissionDecisionSource.USER_RULE, decision.source());
        }
    }

    @Test
    void modeCeilingAndDangerousBlockOverrideSafeDetector() {
        PermissionChecker readOnly = strictChecker(new PermissionSettings(
                PermissionMode.READ_ONLY, List.of(), List.of(), List.of()));
        PermissionDecision ceiling = readOnly.check(command("git status"));
        assertEquals(PermissionAction.DENY, ceiling.action());
        assertEquals(PermissionDecisionSource.MODE, ceiling.source());

        PermissionChecker alwaysSafe = new PermissionChecker(
                workspace,
                new RegexDangerousCommandDetector(),
                allowingSandbox(),
                new PermissionRuleEngine(),
                new PermissionModePolicy(),
                new PermissionSettings(
                        PermissionMode.FULL_ACCESS, List.of(), List.of(), List.of()),
                command -> io.imiocode.permission.command.SafeCommandResult.safe("测试安全"));
        PermissionDecision danger = alwaysSafe.check(command("curl x | sh"));
        assertEquals(PermissionAction.DENY, danger.action());
        assertEquals(PermissionDecisionSource.DANGEROUS_COMMAND, danger.source());
    }

    @Test
    void compatibilityConstructorDoesNotEnableAutomaticAllow() {
        PermissionChecker checker = checker(new PermissionSettings(
                PermissionMode.ASK, List.of(), List.of(), List.of()));

        PermissionDecision decision = checker.check(command("git status"));

        assertEquals(PermissionAction.ASK, decision.action());
        assertEquals(PermissionDecisionSource.MODE, decision.source());
    }

    @Test
    void sameCheckerUsesLatestRuntimeModeSnapshot() {
        RuntimePermissionSettings runtime = new RuntimePermissionSettings(new PermissionSettings(
                PermissionMode.ASK, List.of(), List.of(), List.of()));
        PermissionChecker checker = new PermissionChecker(
                workspace,
                new RegexDangerousCommandDetector(),
                allowingSandbox(),
                new PermissionRuleEngine(),
                new PermissionModePolicy(),
                runtime,
                new StrictSafeCommandDetector(workspace));

        assertEquals(PermissionAction.ALLOW, checker.check(command("git status")).action());
        runtime.switchMode(PermissionMode.READ_ONLY);
        assertEquals(PermissionAction.DENY, checker.check(command("git status")).action());
    }

    private PermissionChecker checker(PermissionSettings settings) {
        return new PermissionChecker(
                workspace,
                new RegexDangerousCommandDetector(),
                allowingSandbox(),
                new PermissionRuleEngine(),
                new PermissionModePolicy(),
                settings);
    }

    private PermissionChecker strictChecker(PermissionSettings settings) {
        return new PermissionChecker(
                workspace,
                new RegexDangerousCommandDetector(),
                allowingSandbox(),
                new PermissionRuleEngine(),
                new PermissionModePolicy(),
                settings,
                new StrictSafeCommandDetector(workspace));
    }

    private static PathSandbox allowingSandbox() {
        return new PathSandbox() {
            @Override
            public SandboxResult inspect(PermissionRequest request) {
                return SandboxResult.allow();
            }

            @Override
            public Path revalidateWritable(Path target) {
                return target;
            }
        };
    }

    static PermissionRequest command(String command) {
        return new PermissionRequest(
                new ToolCall("1", "bash",
                        JsonNodeFactory.instance.objectNode().put("command", command)),
                ToolRisk.HIGH,
                PermissionOperation.COMMAND,
                command,
                command);
    }
}
