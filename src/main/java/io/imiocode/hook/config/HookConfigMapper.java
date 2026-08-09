package io.imiocode.hook.config;

import io.imiocode.config.EnvironmentPlaceholderResolver;
import io.imiocode.hook.Hook;
import io.imiocode.hook.HookEvent;
import io.imiocode.hook.HookFailurePolicy;
import io.imiocode.hook.action.*;
import io.imiocode.hook.condition.ConditionGroup;
import io.imiocode.hook.condition.ConditionParser;
import io.imiocode.hook.condition.DefaultConditionParser;
import io.imiocode.hook.template.HookTemplateResolver;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** 将 YAML 文档完整校验后映射为领域 Hook；禁止部分加载。 */
public final class HookConfigMapper {
    private final ConditionParser parser;
    private final HookValidator validator;
    private final EnvironmentPlaceholderResolver environmentResolver;

    public HookConfigMapper() {
        this.parser = new DefaultConditionParser();
        this.validator = new HookValidator(parser, new HookTemplateResolver());
        this.environmentResolver = new EnvironmentPlaceholderResolver();
    }

    public HookConfigLoadResult load(List<HookDocument> documents, Map<String, String> environment,
                                     Consumer<String> secretRegistrar) {
        if (documents == null || documents.isEmpty()) return HookConfigLoadResult.EMPTY;
        List<Hook> hooks = new ArrayList<>();
        List<HookConfigError> errors = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (int index = 0; index < documents.size(); index++) {
            HookDocument source = documents.get(index);
            String id = source == null ? "" : source.id();
            if (source == null) {
                errors.add(new HookConfigError(index, "", "Hook 不能为空"));
                continue;
            }
            try {
                validator.validate(source);
                if (!ids.add(source.id().trim())) throw new IllegalArgumentException("Hook id 重复");
                hooks.add(map(source, environment, secretRegistrar));
            } catch (RuntimeException exception) {
                errors.add(new HookConfigError(index, id, safeMessage(exception)));
            }
        }
        return errors.isEmpty() ? new HookConfigLoadResult(hooks, List.of())
                : new HookConfigLoadResult(List.of(), errors);
    }

    private Hook map(HookDocument source, Map<String, String> environment,
                     Consumer<String> secretRegistrar) {
        HookEvent event = HookEvent.parse(source.event());
        Optional<ConditionGroup> condition = source.condition() == null || source.condition().isBlank()
                ? Optional.empty() : Optional.of(parser.parse(source.condition()));
        Action action = mapAction(source.action(), environment, secretRegistrar);
        Optional<String> rejectMessage = Optional.ofNullable(source.rejectMessage())
                .map(value -> expand(value, environment, secretRegistrar));
        return new Hook(source.id(), event, condition, action,
                Boolean.TRUE.equals(source.once()), Boolean.TRUE.equals(source.async()),
                Boolean.TRUE.equals(source.reject()), rejectMessage,
                HookFailurePolicy.parse(source.onError()));
    }

    private Action mapAction(ActionDocument source, Map<String, String> environment,
                             Consumer<String> secretRegistrar) {
        HookActionType type = HookActionType.parse(source.type());
        return switch (type) {
            case COMMAND -> new CommandAction(expand(source.command(), environment, secretRegistrar),
                    Duration.ofSeconds(source.timeoutSeconds() == null ? 600 : source.timeoutSeconds()));
            case PROMPT -> new PromptAction(expand(source.message(), environment, secretRegistrar));
            case AGENT -> new AgentAction(expand(source.prompt(), environment, secretRegistrar));
            case HTTP -> {
                Map<String, String> headers = new LinkedHashMap<>();
                if (source.headers() != null) source.headers().forEach((key, value) ->
                        headers.put(key, expand(value, environment, secretRegistrar)));
                String method = source.method() == null || source.method().isBlank() ? "POST" : source.method();
                yield new HttpAction(URI.create(expand(source.url(), environment, secretRegistrar)),
                        method, headers,
                        Optional.ofNullable(source.body()).map(value -> expand(value, environment, secretRegistrar)),
                        Duration.ofSeconds(source.timeoutSeconds() == null ? 10 : source.timeoutSeconds()));
            }
        };
    }

    private String expand(String value, Map<String, String> environment, Consumer<String> registrar) {
        return environmentResolver.expand(value, environment, registrar);
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Hook 配置无效" : message;
    }
}
