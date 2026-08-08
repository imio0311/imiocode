package io.imiocode.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.imiocode.mcp.config.McpConfigDocument;
import io.imiocode.permission.rule.PermissionConfigDocument;

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
        ContextDocument context,
        UiDocument ui,
        InstructionsDocument instructions,
        SessionsDocument sessions,
        MemoryDocument memory,
        SkillsDocument skills,
        McpConfigDocument mcp,
        PermissionConfigDocument permissions,
        Map<String, ProviderConfig> providers) {

    ConfigDocument {
        if (providers == null) {
            providers = Map.of();
        } else {
            providers = Collections.unmodifiableMap(new LinkedHashMap<>(providers));
        }
    }

    static ConfigDocument empty() {
        return new ConfigDocument(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, Map.of());
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
                + ", context=" + context
                + ", ui=" + ui
                + ", instructions=" + instructions
                + ", sessions=" + sessions
                + ", memory=" + memory
                + ", skills=" + (skills == null ? "default" : "configured")
                + ", mcp=" + (mcp == null ? "absent" : "configured")
                + ", permissions=" + (permissions == null ? "absent" : "configured")
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

    record ContextDocument(
            @JsonProperty("window-tokens") Integer windowTokens,
            @JsonProperty("auto-compact-threshold") Double autoCompactThreshold) {
    }

    record UiDocument(String verbosity) {
    }

    record InstructionsDocument(
            Boolean enabled,
            @JsonProperty("max-include-depth") Integer maxIncludeDepth,
            @JsonProperty("max-expanded-bytes") Long maxExpandedBytes) {
    }

    record SessionsDocument(
            Boolean enabled,
            @JsonProperty("retention-days") Integer retentionDays,
            @JsonProperty("max-sessions") Integer maxSessions) {
    }

    record MemoryDocument(
            Boolean enabled,
            @JsonProperty("auto-extract") Boolean autoExtract,
            @JsonProperty("user-scope-enabled") Boolean userScopeEnabled,
            @JsonProperty("project-scope-enabled") Boolean projectScopeEnabled,
            @JsonProperty("max-entries-per-scope") Integer maxEntriesPerScope,
            @JsonProperty("max-entry-chars") Integer maxEntryChars,
            @JsonProperty("max-file-bytes") Long maxFileBytes,
            @JsonProperty("extraction-output-tokens") Integer extractionOutputTokens) {
    }

    record SkillsDocument(InstallDocument install) {
    }

    record InstallDocument(
            @JsonProperty("timeout-seconds") Integer timeoutSeconds,
            @JsonProperty("max-files") Integer maxFiles,
            @JsonProperty("max-file-bytes") Long maxFileBytes,
            @JsonProperty("max-total-bytes") Long maxTotalBytes,
            @JsonProperty("allowed-hosts") java.util.Set<String> allowedHosts) {
    }
}
