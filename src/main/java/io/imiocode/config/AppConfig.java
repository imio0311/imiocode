package io.imiocode.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public record AppConfig(
        Provider provider,
        String model,
        String apiKey,
        URI baseUri,
        Duration connectTimeout,
        Duration requestTimeout,
        int maxOutputTokens,
        ThinkingConfig thinking) {

    public AppConfig {
        Objects.requireNonNull(provider, "provider");
        model = requireText(model, "model");
        apiKey = requireText(apiKey, "apiKey");
        Objects.requireNonNull(baseUri, "baseUri");
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        Objects.requireNonNull(thinking, "thinking");
        if (!baseUri.isAbsolute()) {
            throw new IllegalArgumentException("baseUri 必须是绝对 URI");
        }
        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalArgumentException("connectTimeout 必须为正数");
        }
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout 必须为正数");
        }
        if (maxOutputTokens <= 0) {
            throw new IllegalArgumentException("maxOutputTokens 必须为正数");
        }
    }

    public AppConfig(
            Provider provider,
            String model,
            String apiKey,
            URI baseUri,
            Duration connectTimeout,
            Duration requestTimeout,
            int maxOutputTokens) {
        this(provider, model, apiKey, baseUri, connectTimeout, requestTimeout, maxOutputTokens,
                ThinkingConfig.disabled());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return "AppConfig[provider=" + provider
                + ", model=" + model
                + ", apiKey=***"
                + ", baseUri=" + baseUri
                + ", connectTimeout=" + connectTimeout
                + ", requestTimeout=" + requestTimeout
                + ", maxOutputTokens=" + maxOutputTokens
                + ", thinking=" + thinking + "]";
    }
}
