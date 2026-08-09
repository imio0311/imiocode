package io.imiocode.hook.config;

import io.imiocode.hook.HookEvent;
import io.imiocode.hook.HookFailurePolicy;
import io.imiocode.hook.action.HookActionType;
import io.imiocode.hook.condition.ConditionParser;
import io.imiocode.hook.template.HookTemplateResolver;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 单项 Hook 的结构与交叉约束校验。 */
public final class HookValidator {
    public static final int MAX_TIMEOUT_SECONDS = 3600;
    private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private final ConditionParser conditionParser;
    private final HookTemplateResolver templates;

    public HookValidator(ConditionParser conditionParser, HookTemplateResolver templates) {
        this.conditionParser = conditionParser;
        this.templates = templates;
    }

    public void validate(HookDocument document) {
        requireText(document.id(), "id");
        HookEvent event = HookEvent.parse(document.event());
        if (document.condition() != null && !document.condition().isBlank()) conditionParser.parse(document.condition());
        HookFailurePolicy policy = HookFailurePolicy.parse(document.onError());
        boolean reject = Boolean.TRUE.equals(document.reject());
        boolean async = Boolean.TRUE.equals(document.async());
        if ((reject || document.rejectMessage() != null || policy == HookFailurePolicy.REJECT)
                && event != HookEvent.PRE_TOOL_USE)
            throw new IllegalArgumentException("reject/reject-message/on-error=reject 仅允许 pre_tool_use");
        if (event == HookEvent.PRE_TOOL_USE && async)
            throw new IllegalArgumentException("pre_tool_use 禁止 async=true");
        if (reject && async) throw new IllegalArgumentException("reject Hook 禁止 async=true");
        if (document.rejectMessage() != null) templates.validate(document.rejectMessage());
        ActionDocument action = document.action();
        if (action == null) throw new IllegalArgumentException("action 不能为空");
        HookActionType type = HookActionType.parse(action.type());
        validateTimeout(action.timeoutSeconds());
        switch (type) {
            case COMMAND -> { requireText(action.command(), "action.command"); templates.validate(action.command()); }
            case PROMPT -> { requireText(action.message(), "action.message"); templates.validate(action.message()); }
            case AGENT -> { requireText(action.prompt(), "action.prompt"); templates.validate(action.prompt()); }
            case HTTP -> validateHttp(action);
        }
    }

    private void validateHttp(ActionDocument action) {
        String url = requireText(action.url(), "action.url");
        templates.validate(url);
        URI uri;
        try { uri = URI.create(url); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("action.url 无效"); }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")))
            throw new IllegalArgumentException("action.url 仅允许 http/https");
        String method = action.method() == null || action.method().isBlank()
                ? "POST" : action.method().trim().toUpperCase(Locale.ROOT);
        if (!HTTP_METHODS.contains(method)) throw new IllegalArgumentException("不支持的 HTTP method: " + method);
        if (action.body() != null) templates.validate(action.body());
        if (action.headers() != null) action.headers().forEach((key, value) -> {
            requireText(key, "HTTP header 名");
            if (value == null) throw new IllegalArgumentException("HTTP header 值不能为空");
            templates.validate(value);
        });
    }

    private static void validateTimeout(Integer seconds) {
        if (seconds != null && (seconds <= 0 || seconds > MAX_TIMEOUT_SECONDS))
            throw new IllegalArgumentException("action.timeout-seconds 必须在 1.." + MAX_TIMEOUT_SECONDS + " 之间");
    }

    static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " 不能为空");
        return value.trim();
    }
}
