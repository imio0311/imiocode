package io.imiocode.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

record ConfigDocument(
        String provider,
        String model,
        @JsonProperty("connect-timeout-seconds") Integer connectTimeoutSeconds,
        @JsonProperty("request-timeout-seconds") Integer requestTimeoutSeconds,
        @JsonProperty("max-output-tokens") Integer maxOutputTokens,
        ThinkingDocument thinking,
        AgentDocument agent,
        Map<String, ProviderConfig> providers) {

    ConfigDocument {
        if (providers == null) {
            providers = Map.of();
        } else {
            providers = Collections.unmodifiableMap(new LinkedHashMap<>(providers));
        }
    }

    static ConfigDocument empty() {
        return new ConfigDocument(null, null, null, null, null, null, null, Map.of());
    }

    ProviderConfig providerConfig(Provider selectedProvider) {
        return providers.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).equals(selectedProvider.configValue()))
                .map(entry -> entry.getValue() == null ? ProviderConfig.empty() : entry.getValue())
                .findFirst()
                .orElseGet(ProviderConfig::empty);
    }

    @Override
    public String toString() {
        return "ConfigDocument[provider=" + provider
                + ", model=" + model
                + ", connectTimeoutSeconds=" + connectTimeoutSeconds
                + ", requestTimeoutSeconds=" + requestTimeoutSeconds
                + ", maxOutputTokens=" + maxOutputTokens
                + ", thinking=" + thinking
                + ", agent=" + agent
                + ", providers=***]";
    }

    record ThinkingDocument(
            Boolean enabled,
            String mode,
            @JsonProperty("budget-tokens") Integer budgetTokens,
            String effort,
            String summary) {
    }

    record AgentDocument(
            @JsonProperty("max-iterations") Integer maxIterations,
            @JsonProperty("timeout-seconds") Integer timeoutSeconds,
            @JsonProperty("max-parallel-tools") Integer maxParallelTools) {
    }
}
