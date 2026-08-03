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
        ThinkingConfig thinking,
        AgentConfig agent,
        ContextConfig context,
        UiConfig ui,
        InstructionsConfig instructions,
        SessionsConfig sessions,
        MemoryConfig memory) {

    public AppConfig {
        Objects.requireNonNull(provider, "provider");
        model = requireText(model, "model");
        apiKey = requireText(apiKey, "apiKey");
        Objects.requireNonNull(baseUri, "baseUri");
        Objects.requireNonNull(connectTimeout, "connectTimeout");
        Objects.requireNonNull(requestTimeout, "requestTimeout");
        Objects.requireNonNull(thinking, "thinking");
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(ui, "ui");
        Objects.requireNonNull(instructions, "instructions");
        Objects.requireNonNull(sessions, "sessions");
        Objects.requireNonNull(memory, "memory");
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
        if (context.windowTokens() <= maxOutputTokens) {
            throw new IllegalArgumentException("context.windowTokens 必须大于 maxOutputTokens");
        }
    }

    public AppConfig(
            Provider provider,
            String model,
            String apiKey,
            URI baseUri,
            Duration connectTimeout,
            Duration requestTimeout,
            int maxOutputTokens,
            ThinkingConfig thinking,
            AgentConfig agent,
            ContextConfig context,
            UiConfig ui) {
        this(provider, model, apiKey, baseUri, connectTimeout, requestTimeout, maxOutputTokens,
                thinking, agent, context, ui,
                InstructionsConfig.defaults(), SessionsConfig.defaults(), MemoryConfig.defaults());
    }

    public AppConfig(
            Provider provider,
            String model,
            String apiKey,
            URI baseUri,
            Duration connectTimeout,
            Duration requestTimeout,
            int maxOutputTokens,
            ThinkingConfig thinking,
            AgentConfig agent,
            ContextConfig context) {
        this(provider, model, apiKey, baseUri, connectTimeout, requestTimeout, maxOutputTokens,
                thinking, agent, context, UiConfig.defaults());
    }

    public AppConfig(
            Provider provider,
            String model,
            String apiKey,
            URI baseUri,
            Duration connectTimeout,
            Duration requestTimeout,
            int maxOutputTokens,
            ThinkingConfig thinking,
            AgentConfig agent) {
        this(provider, model, apiKey, baseUri, connectTimeout, requestTimeout, maxOutputTokens,
                thinking, agent, ContextConfig.defaults(), UiConfig.defaults());
    }

    public AppConfig(
            Provider provider,
            String model,
            String apiKey,
            URI baseUri,
            Duration connectTimeout,
            Duration requestTimeout,
            int maxOutputTokens,
            ThinkingConfig thinking) {
        this(provider, model, apiKey, baseUri, connectTimeout, requestTimeout, maxOutputTokens,
                thinking, AgentConfig.defaults(), ContextConfig.defaults(), UiConfig.defaults());
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
                ThinkingConfig.disabled(), AgentConfig.defaults(), ContextConfig.defaults(), UiConfig.defaults());
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
                + ", thinking=" + thinking
                + ", agent=" + agent
                + ", context=" + context
                + ", ui=" + ui
                + ", instructions=" + instructions
                + ", sessions=" + sessions
                + ", memory=" + memory + "]";
    }
}
