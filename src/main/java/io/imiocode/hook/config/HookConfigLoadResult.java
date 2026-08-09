package io.imiocode.hook.config;

import io.imiocode.hook.Hook;
import java.util.List;
import java.util.Objects;

public record HookConfigLoadResult(List<Hook> hooks, List<HookConfigError> errors) {
    public static final HookConfigLoadResult EMPTY = new HookConfigLoadResult(List.of(), List.of());
    public HookConfigLoadResult {
        hooks = List.copyOf(Objects.requireNonNullElse(hooks, List.of()));
        errors = List.copyOf(Objects.requireNonNullElse(errors, List.of()));
        if (!errors.isEmpty() && !hooks.isEmpty()) throw new IllegalArgumentException("配置错误时不能部分加载 Hook");
    }
}
