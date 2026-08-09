package io.imiocode.hook.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imiocode.hook.HookActionResult;
import io.imiocode.hook.HookContext;
import io.imiocode.hook.HookExecutionStatus;
import io.imiocode.hook.template.HookTemplateResolver;
import io.imiocode.tool.SecretRedactor;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 执行模板化 HTTP 请求，日志与结果中不暴露请求头。 */
public final class HttpHookExecutor implements HookActionExecutor<HttpAction>, AutoCloseable {
    private final HookTemplateResolver templates;
    private final HookHttpTransport transport;
    private final SecretRedactor redactor;
    private final ObjectMapper mapper;

    public HttpHookExecutor(HookTemplateResolver templates, HookHttpTransport transport, SecretRedactor redactor) {
        this.templates = templates;
        this.transport = transport;
        this.redactor = redactor;
        this.mapper = new ObjectMapper();
    }

    @Override public HookActionType type() { return HookActionType.HTTP; }

    @Override
    public HookActionResult execute(HttpAction action, HookContext context) {
        URI uri;
        try {
            uri = URI.create(templates.resolve(action.url().toString(), context));
        } catch (IllegalArgumentException exception) {
            return HookActionResult.failure("HTTP Hook URL 无效", java.time.Duration.ZERO);
        }
        Map<String, String> headers = new LinkedHashMap<>();
        action.headers().forEach((name, value) -> headers.put(name, templates.resolve(value, context)));
        String body = action.body().map(value -> templates.resolve(value, context)).orElseGet(() -> defaultBody(context));
        HookHttpResult result = transport.send(new HookHttpRequest(uri, action.method(), headers, body, action.timeout()));
        String safeBody = redactor.redact(result.body());
        if (result.timedOut()) {
            return new HookActionResult(HookExecutionStatus.TIMED_OUT, "", Optional.of("HTTP Hook 请求超时"), result.elapsed());
        }
        if (!result.sent()) return HookActionResult.failure(redactor.redact(result.safeError()), result.elapsed());
        if (result.tooLarge()) return HookActionResult.failure("HTTP Hook 响应超过 1 MiB", result.elapsed());
        if (result.statusCode() < 200 || result.statusCode() >= 300) {
            return HookActionResult.failure("HTTP Hook 返回状态码 " + result.statusCode(), result.elapsed());
        }
        return HookActionResult.success(safeBody, result.elapsed());
    }

    private String defaultBody(HookContext context) {
        try {
            return mapper.writeValueAsString(context.safePayload(redactor));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    @Override public void close() throws Exception {
        if (transport instanceof AutoCloseable closeable) closeable.close();
    }
}
