package io.imiocode.permission;

import io.imiocode.permission.command.DangerousCommandDetector;
import io.imiocode.permission.command.DangerousCommandMatch;
import io.imiocode.permission.command.SafeCommandDetector;
import io.imiocode.permission.command.SafeCommandResult;
import io.imiocode.permission.rule.PermissionRuleEngine;
import io.imiocode.permission.sandbox.PathSandbox;
import io.imiocode.permission.sandbox.SandboxResult;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** 串联硬拦截、沙箱、模式上限、规则和模式默认值。 */
public final class PermissionChecker {
    private final Path workspace;
    private final DangerousCommandDetector commandDetector;
    private final PathSandbox sandbox;
    private final PermissionRuleEngine ruleEngine;
    private final PermissionModePolicy modePolicy;
    private final PermissionSettingsProvider settingsProvider;
    private final SafeCommandDetector safeCommandDetector;

    public PermissionChecker(
            Path workspace,
            DangerousCommandDetector commandDetector,
            PathSandbox sandbox,
            PermissionRuleEngine ruleEngine,
            PermissionModePolicy modePolicy,
            PermissionSettings settings
    ) {
        this(
                workspace,
                commandDetector,
                sandbox,
                ruleEngine,
                modePolicy,
                fixed(settings),
                command -> SafeCommandResult.uncertain("兼容模式未启用安全命令自动放行"));
    }

    public PermissionChecker(
            Path workspace,
            DangerousCommandDetector commandDetector,
            PathSandbox sandbox,
            PermissionRuleEngine ruleEngine,
            PermissionModePolicy modePolicy,
            PermissionSettings settings,
            SafeCommandDetector safeCommandDetector
    ) {
        this(workspace, commandDetector, sandbox, ruleEngine, modePolicy, fixed(settings),
                safeCommandDetector);
    }

    public PermissionChecker(
            Path workspace,
            DangerousCommandDetector commandDetector,
            PathSandbox sandbox,
            PermissionRuleEngine ruleEngine,
            PermissionModePolicy modePolicy,
            PermissionSettingsProvider settingsProvider,
            SafeCommandDetector safeCommandDetector
    ) {
        this.workspace = Objects.requireNonNull(workspace, "workspace 不能为空")
                .toAbsolutePath().normalize();
        this.commandDetector = Objects.requireNonNull(commandDetector, "commandDetector 不能为空");
        this.sandbox = Objects.requireNonNull(sandbox, "sandbox 不能为空");
        this.ruleEngine = Objects.requireNonNull(ruleEngine, "ruleEngine 不能为空");
        this.modePolicy = Objects.requireNonNull(modePolicy, "modePolicy 不能为空");
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider 不能为空");
        this.safeCommandDetector =
                Objects.requireNonNull(safeCommandDetector, "safeCommandDetector 不能为空");
    }

    public PermissionDecision check(PermissionRequest request) {
        try {
            Objects.requireNonNull(request, "request 不能为空");
            PermissionSettings settings = Objects.requireNonNull(
                    settingsProvider.snapshot(), "权限设置快照不能为空");
            if (request.operation() == PermissionOperation.COMMAND) {
                Optional<DangerousCommandMatch> danger =
                        commandDetector.inspect(request.normalizedTarget(), workspace);
                if (danger.isPresent()) {
                    return PermissionDecision.deny(
                            PermissionDecisionSource.DANGEROUS_COMMAND,
                            "危险命令已硬拦截：" + danger.orElseThrow().reason());
                }
            }

            SandboxResult sandboxResult = sandbox.inspect(request);
            if (!sandboxResult.allowed()) {
                return PermissionDecision.deny(
                        PermissionDecisionSource.SANDBOX, sandboxResult.reason());
            }

            PermissionDecision ceiling = modePolicy.ceiling(request, settings.mode());
            if (ceiling != null) {
                return ceiling;
            }

            Optional<PermissionDecision> rule = ruleEngine.evaluate(request, settings);
            if (rule.isPresent()) {
                PermissionDecision ruled = rule.orElseThrow();
                if (!forceInstallConfirmation(request, settings) || ruled.action() != PermissionAction.ALLOW) {
                    return ruled;
                }
                return PermissionDecision.ask(
                        PermissionDecisionSource.MODE, "强制覆盖 Skill 需要用户确认");
            }

            if (forceInstallConfirmation(request, settings)) {
                return PermissionDecision.ask(
                        PermissionDecisionSource.MODE, "强制覆盖 Skill 需要用户确认");
            }

            if (request.operation() == PermissionOperation.COMMAND) {
                SafeCommandResult safe =
                        safeCommandDetector.inspect(request.normalizedTarget());
                if (safe.safe()) {
                    return PermissionDecision.allow(
                            PermissionDecisionSource.SAFE_COMMAND, safe.reason());
                }
            }
            return modePolicy.evaluate(request, settings.mode());
        } catch (RuntimeException exception) {
            return PermissionDecision.deny(
                    PermissionDecisionSource.ERROR, "权限检查失败，已安全拒绝");
        }
    }

    private static PermissionSettingsProvider fixed(PermissionSettings settings) {
        PermissionSettings checked = Objects.requireNonNull(settings, "settings 不能为空");
        return () -> checked;
    }

    private static boolean forceInstallConfirmation(
            PermissionRequest request, PermissionSettings settings) {
        return "install_skill".equalsIgnoreCase(request.call().name())
                && request.risk() == io.imiocode.tool.ToolRisk.HIGH
                && settings.mode() != PermissionMode.FULL_ACCESS;
    }
}
