package io.imiocode.hook.action;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record HttpAction(URI url, String method, Map<String, String> headers,
                         Optional<String> body, Duration timeout) implements Action {
    public HttpAction {
        url = Objects.requireNonNull(url, "url");
        method = Objects.requireNonNull(method, "method").trim().toUpperCase(java.util.Locale.ROOT);
        headers = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNullElse(headers, Map.of())));
        body = Objects.requireNonNullElse(body, Optional.empty());
        timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout 必须大于 0");
    }
    @Override public HookActionType type() { return HookActionType.HTTP; }
}
