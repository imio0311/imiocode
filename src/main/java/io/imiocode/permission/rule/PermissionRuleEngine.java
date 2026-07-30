package io.imiocode.permission.rule;

import io.imiocode.permission.PermissionDecision;
import io.imiocode.permission.PermissionDecisionSource;
import io.imiocode.permission.PermissionRequest;
import io.imiocode.permission.PermissionRule;
import io.imiocode.permission.PermissionRuleLayer;
import io.imiocode.permission.PermissionSettings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 严格按层级和书写顺序执行第一条匹配规则。 */
public final class PermissionRuleEngine {
    private final PermissionGlobMatcher matcher;

    public PermissionRuleEngine() {
        this(new PermissionGlobMatcher());
    }

    public PermissionRuleEngine(PermissionGlobMatcher matcher) {
        this.matcher = Objects.requireNonNull(matcher, "matcher 不能为空");
    }

    public Optional<PermissionDecision> evaluate(
            PermissionRequest request,
            PermissionSettings settings
    ) {
        Objects.requireNonNull(request, "request 不能为空");
        Objects.requireNonNull(settings, "settings 不能为空");
        Optional<PermissionDecision> user = firstMatch(request, settings.userRules());
        if (user.isPresent()) {
            return user;
        }
        Optional<PermissionDecision> project = firstMatch(request, settings.projectRules());
        if (project.isPresent()) {
            return project;
        }
        return firstMatch(request, settings.localRules());
    }

    private Optional<PermissionDecision> firstMatch(
            PermissionRequest request,
            List<PermissionRule> rules
    ) {
        for (PermissionRule rule : rules) {
            if (!matcher.matches(rule.toolPattern(), request.call().name())) {
                continue;
            }
            if (rule.targetPattern().isPresent()
                    && !matcher.matches(
                    rule.targetPattern().orElseThrow(), request.normalizedTarget())) {
                continue;
            }
            PermissionDecisionSource source = source(rule.layer());
            String reason = "命中" + layerName(rule.layer()) + "权限规则";
            return Optional.of(switch (rule.action()) {
                case ALLOW -> PermissionDecision.allow(source, reason);
                case ASK -> PermissionDecision.ask(source, reason);
                case DENY -> PermissionDecision.deny(source, reason);
            });
        }
        return Optional.empty();
    }

    private static PermissionDecisionSource source(PermissionRuleLayer layer) {
        return switch (layer) {
            case USER -> PermissionDecisionSource.USER_RULE;
            case PROJECT -> PermissionDecisionSource.PROJECT_RULE;
            case LOCAL -> PermissionDecisionSource.LOCAL_RULE;
        };
    }

    private static String layerName(PermissionRuleLayer layer) {
        return switch (layer) {
            case USER -> "用户级";
            case PROJECT -> "项目级";
            case LOCAL -> "本地级";
        };
    }
}
