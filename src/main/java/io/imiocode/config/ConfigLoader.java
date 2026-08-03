package io.imiocode.config;

import io.imiocode.mcp.config.McpConfigLoadResult;
import io.imiocode.mcp.config.McpConfigLoader;
import io.imiocode.permission.PermissionSettings;
import io.imiocode.permission.rule.PermissionRuleLoader;
import io.imiocode.tool.SecretRedactor;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class ConfigLoader {
    static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(120);
    static final int DEFAULT_MAX_OUTPUT_TOKENS = 4096;
    static final int DEFAULT_THINKING_BUDGET_TOKENS = 1024;
    private final YamlConfigLoader yamlConfigLoader;
    private final EnvironmentPlaceholderResolver placeholderResolver;
    private final McpConfigLoader mcpConfigLoader;
    private final PermissionRuleLoader permissionRuleLoader;

    public ConfigLoader() {
        this(
                new YamlConfigLoader(),
                new EnvironmentPlaceholderResolver(),
                new McpConfigLoader(),
                new PermissionRuleLoader());
    }

    ConfigLoader(YamlConfigLoader yamlConfigLoader) {
        this(
                yamlConfigLoader,
                new EnvironmentPlaceholderResolver(),
                new McpConfigLoader(),
                new PermissionRuleLoader());
    }

    ConfigLoader(
            YamlConfigLoader yamlConfigLoader,
            EnvironmentPlaceholderResolver placeholderResolver) {
        this(
                yamlConfigLoader,
                placeholderResolver,
                new McpConfigLoader(),
                new PermissionRuleLoader());
    }

    ConfigLoader(
            YamlConfigLoader yamlConfigLoader,
            EnvironmentPlaceholderResolver placeholderResolver,
            McpConfigLoader mcpConfigLoader,
            PermissionRuleLoader permissionRuleLoader) {
        this.yamlConfigLoader = Objects.requireNonNull(yamlConfigLoader, "yamlConfigLoader");
        this.placeholderResolver = Objects.requireNonNull(placeholderResolver, "placeholderResolver");
        this.mcpConfigLoader = Objects.requireNonNull(mcpConfigLoader, "mcpConfigLoader");
        this.permissionRuleLoader = Objects.requireNonNull(permissionRuleLoader, "permissionRuleLoader");
    }

    public AppConfig load(Map<String, String> environment) {
        return load(Path.of("").toAbsolutePath(), environment);
    }

    public AppConfig load(Path workingDirectory, Map<String, String> environment) {
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Objects.requireNonNull(environment, "environment");
        ConfigDocument document = yamlConfigLoader.load(workingDirectory);
        return buildAppConfig(document, environment, ignored -> { });
    }

    /** 一次读取根配置并装配应用、MCP、权限及共享脱敏器。 */
    public RuntimeConfig loadAll(
            Path workspace,
            Path userHome,
            Map<String, String> environment) {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(userHome, "userHome");
        Objects.requireNonNull(environment, "environment");

        Path normalizedWorkspace = workspace.toAbsolutePath().normalize();
        Path normalizedUserHome = userHome.toAbsolutePath().normalize();
        Path configPath = normalizedWorkspace.resolve(YamlConfigLoader.FILE_NAME);
        ConfigDocument document = yamlConfigLoader.load(normalizedWorkspace);

        List<String> expandedSecrets = new ArrayList<>();
        AppConfig app = buildAppConfig(document, environment, expandedSecrets::add);
        SecretRedactor redactor = new SecretRedactor(app.apiKey());
        expandedSecrets.forEach(redactor::registerSecret);

        List<ConfigNotice> notices = new ArrayList<>();
        McpConfigLoadResult mcp;
        ConfigSource mcpSource;
        if (document.mcp() != null) {
            mcp = mcpConfigLoader.loadUnified(
                    document.mcp(), configPath, environment, redactor);
            mcpSource = ConfigSource.UNIFIED;
        } else {
            boolean legacyPresent = hasLegacyMcpConfig(normalizedWorkspace, normalizedUserHome);
            mcp = mcpConfigLoader.loadLegacy(
                    normalizedWorkspace, normalizedUserHome, environment, redactor);
            mcpSource = legacyPresent ? ConfigSource.LEGACY : ConfigSource.DEFAULT;
            if (legacyPresent) {
                notices.add(new ConfigNotice(
                        "legacy_mcp_config",
                        "[配置] 正在兼容读取旧 MCP 配置，建议迁移到 config.yaml 的 mcp 区域。"));
            }
        }

        PermissionSettings permissions;
        ConfigSource permissionSource;
        if (document.permissions() != null) {
            permissions = permissionRuleLoader.loadUnified(document.permissions());
            permissionSource = ConfigSource.UNIFIED;
        } else {
            boolean legacyPresent = hasLegacyPermissionConfig(normalizedWorkspace, normalizedUserHome);
            permissions = permissionRuleLoader.loadLegacy(normalizedWorkspace, normalizedUserHome);
            permissionSource = legacyPresent ? ConfigSource.LEGACY : ConfigSource.DEFAULT;
            if (legacyPresent) {
                notices.add(new ConfigNotice(
                        "legacy_permission_config",
                        "[配置] 正在兼容读取旧权限配置，建议迁移到 config.yaml 的 permissions 区域。"));
            }
        }

        ConfigSource appSource = Files.exists(configPath, LinkOption.NOFOLLOW_LINKS)
                ? ConfigSource.UNIFIED
                : ConfigSource.DEFAULT;
        return new RuntimeConfig(
                app,
                mcp,
                permissions,
                redactor,
                new ConfigSourceSummary(appSource, mcpSource, permissionSource),
                notices);
    }

    private AppConfig buildAppConfig(
            ConfigDocument document,
            Map<String, String> environment,
            Consumer<String> secretRegistrar) {
        Provider provider = Provider.parse(firstNonBlank(environment.get("IMIO_PROVIDER"), document.provider()));
        String model = required(firstNonBlank(environment.get("IMIO_MODEL"), document.model()), "IMIO_MODEL / model");
        ProviderConfig providerConfig = document.providerConfig(provider);
        String apiKeyName = apiKeyName(provider);
        String configuredApiKey = resolveYamlValue(
                environment.get(apiKeyName),
                providerConfig.apiKey(),
                environment,
                secretRegistrar,
                "providers." + provider.configValue() + ".api-key");
        String apiKey = required(
                configuredApiKey,
                apiKeyName + " / providers." + provider.configValue() + ".api-key");
        String baseUrlName = baseUrlName(provider);
        String configuredBaseUrl = resolveYamlValue(
                environment.get(baseUrlName),
                providerConfig.baseUrl(),
                environment,
                ignored -> { },
                "providers." + provider.configValue() + ".base-url");
        URI baseUri = parseUri(
                configuredBaseUrl,
                defaultBaseUri(provider),
                baseUrlName + " / providers." + provider.configValue() + ".base-url");
        Duration connectTimeout = Duration.ofSeconds(mergePositiveInt(
                environment.get("IMIO_CONNECT_TIMEOUT_SECONDS"),
                document.connectTimeoutSeconds(),
                Math.toIntExact(DEFAULT_CONNECT_TIMEOUT.toSeconds()),
                "IMIO_CONNECT_TIMEOUT_SECONDS"));
        Duration requestTimeout = Duration.ofSeconds(mergePositiveInt(
                environment.get("IMIO_REQUEST_TIMEOUT_SECONDS"),
                document.requestTimeoutSeconds(),
                Math.toIntExact(DEFAULT_REQUEST_TIMEOUT.toSeconds()),
                "IMIO_REQUEST_TIMEOUT_SECONDS"));
        int maxOutputTokens = mergePositiveInt(
                environment.get("IMIO_MAX_OUTPUT_TOKENS"),
                document.maxOutputTokens(),
                DEFAULT_MAX_OUTPUT_TOKENS,
                "IMIO_MAX_OUTPUT_TOKENS");
        ConfigDocument.ThinkingDocument thinkingDocument = document.thinking();
        boolean thinkingEnabled = mergeBoolean(
                environment.get("IMIO_THINKING_ENABLED"),
                thinkingDocument == null ? null : thinkingDocument.enabled(),
                false,
                "IMIO_THINKING_ENABLED");
        ThinkingMode thinkingMode = ThinkingMode.parse(firstNonBlank(
                environment.get("IMIO_THINKING_MODE"),
                thinkingDocument == null ? null : thinkingDocument.mode()));
        int thinkingBudget = mergePositiveInt(
                environment.get("IMIO_THINKING_BUDGET_TOKENS"),
                thinkingDocument == null ? null : thinkingDocument.budgetTokens(),
                DEFAULT_THINKING_BUDGET_TOKENS,
                "IMIO_THINKING_BUDGET_TOKENS");
        ReasoningEffort reasoningEffort = ReasoningEffort.parse(firstNonBlank(
                environment.get("IMIO_REASONING_EFFORT"),
                thinkingDocument == null ? null : thinkingDocument.effort()));
        ReasoningSummary reasoningSummary = ReasoningSummary.parse(firstNonBlank(
                environment.get("IMIO_REASONING_SUMMARY"),
                thinkingDocument == null ? null : thinkingDocument.summary()));
        if (thinkingEnabled && thinkingMode == ThinkingMode.MANUAL && thinkingBudget < 1024) {
            throw new ConfigException("配置项 thinking.budget-tokens / IMIO_THINKING_BUDGET_TOKENS 不能小于 1024");
        }
        if (thinkingEnabled && provider == Provider.ANTHROPIC
                && thinkingMode == ThinkingMode.MANUAL && thinkingBudget >= maxOutputTokens) {
            throw new ConfigException("Anthropic manual Thinking 预算必须小于 max-output-tokens");
        }
        ThinkingConfig thinking = new ThinkingConfig(
                thinkingEnabled, thinkingMode, thinkingBudget, reasoningEffort, reasoningSummary);
        ConfigDocument.AgentDocument agentDocument = document.agent();
        int maxIterations = mergePositiveInt(
                environment.get("IMIO_AGENT_MAX_ITERATIONS"),
                agentDocument == null ? null : agentDocument.maxIterations(),
                AgentConfig.DEFAULT_MAX_ITERATIONS,
                "IMIO_AGENT_MAX_ITERATIONS");
        Duration taskTimeout = Duration.ofSeconds(mergePositiveInt(
                environment.get("IMIO_AGENT_TIMEOUT_SECONDS"),
                agentDocument == null ? null : agentDocument.timeoutSeconds(),
                Math.toIntExact(AgentConfig.DEFAULT_TASK_TIMEOUT.toSeconds()),
                "IMIO_AGENT_TIMEOUT_SECONDS"));
        int maxParallelTools = mergePositiveInt(
                environment.get("IMIO_AGENT_MAX_PARALLEL_TOOLS"),
                agentDocument == null ? null : agentDocument.maxParallelTools(),
                AgentConfig.DEFAULT_MAX_PARALLEL_TOOLS,
                "IMIO_AGENT_MAX_PARALLEL_TOOLS");
        AgentConfig agent = new AgentConfig(maxIterations, taskTimeout, maxParallelTools);
        ConfigDocument.ContextDocument contextDocument = document.context();
        int contextWindowTokens = mergePositiveInt(
                environment.get("IMIO_CONTEXT_WINDOW_TOKENS"),
                contextDocument == null ? null : contextDocument.windowTokens(),
                ContextConfig.DEFAULT_WINDOW_TOKENS,
                "IMIO_CONTEXT_WINDOW_TOKENS");
        double autoCompactThreshold = mergeThreshold(
                environment.get("IMIO_CONTEXT_AUTO_COMPACT_THRESHOLD"),
                contextDocument == null ? null : contextDocument.autoCompactThreshold(),
                ContextConfig.DEFAULT_AUTO_COMPACT_THRESHOLD,
                "IMIO_CONTEXT_AUTO_COMPACT_THRESHOLD");
        if (contextWindowTokens <= maxOutputTokens) {
            throw new ConfigException("配置项 context.window-tokens 必须大于 max-output-tokens");
        }
        ContextConfig context = new ContextConfig(contextWindowTokens, autoCompactThreshold);
        ConfigDocument.UiDocument uiDocument = document.ui();
        UiConfig ui = new UiConfig(UiVerbosity.parse(
                uiDocument == null ? null : uiDocument.verbosity()));
        InstructionsConfig instructions = buildInstructionsConfig(document.instructions());
        SessionsConfig sessions = buildSessionsConfig(document.sessions());
        MemoryConfig memory = buildMemoryConfig(document.memory());

        try {
            return new AppConfig(
                    provider, model, apiKey, baseUri, connectTimeout, requestTimeout,
                    maxOutputTokens, thinking, agent, context, ui,
                    instructions, sessions, memory);
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置无效：" + exception.getMessage(), exception);
        }
    }

    private static boolean mergeBoolean(
            String environmentValue,
            Boolean yamlValue,
            boolean defaultValue,
            String name) {
        if (environmentValue == null || environmentValue.isBlank()) {
            return yamlValue == null ? defaultValue : yamlValue;
        }
        String normalized = environmentValue.trim().toLowerCase(java.util.Locale.ROOT);
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        throw new ConfigException("配置项 " + name + " 必须是 true 或 false");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ConfigException("缺少配置项 " + name);
        }
        return value.trim();
    }

    private static URI parseUri(String value, URI defaultValue, String name) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            URI uri = new URI(value.trim());
            if (!uri.isAbsolute() || uri.getHost() == null) {
                throw new URISyntaxException(value, "必须包含协议和主机");
            }
            return stripTrailingSlash(uri);
        } catch (URISyntaxException exception) {
            throw new ConfigException("配置项 " + name + " 不是有效的绝对 URI", exception);
        }
    }

    private static int mergePositiveInt(String environmentValue, Integer yamlValue, int defaultValue, String name) {
        if (environmentValue != null && !environmentValue.isBlank()) {
            return parsePositiveInt(environmentValue, name);
        }
        if (yamlValue != null) {
            if (yamlValue <= 0) {
                throw new ConfigException("config.yaml 配置项 " + yamlName(name) + " 必须是正整数");
            }
            return yamlValue;
        }
        return defaultValue;
    }

    private static int parsePositiveInt(String value, String name) {
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ConfigException("配置项 " + name + " 必须是正整数", exception);
        }
    }

    private static InstructionsConfig buildInstructionsConfig(
            ConfigDocument.InstructionsDocument document) {
        InstructionsConfig defaults = InstructionsConfig.defaults();
        try {
            return new InstructionsConfig(
                    document == null || document.enabled() == null ? defaults.enabled() : document.enabled(),
                    document == null || document.maxIncludeDepth() == null
                            ? defaults.maxIncludeDepth() : document.maxIncludeDepth(),
                    document == null || document.maxExpandedBytes() == null
                            ? defaults.maxExpandedBytes() : document.maxExpandedBytes());
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置无效：" + exception.getMessage(), exception);
        }
    }

    private static SessionsConfig buildSessionsConfig(ConfigDocument.SessionsDocument document) {
        SessionsConfig defaults = SessionsConfig.defaults();
        try {
            return new SessionsConfig(
                    document == null || document.enabled() == null ? defaults.enabled() : document.enabled(),
                    document == null || document.retentionDays() == null
                            ? defaults.retentionDays() : document.retentionDays(),
                    document == null || document.maxSessions() == null
                            ? defaults.maxSessions() : document.maxSessions());
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置无效：" + exception.getMessage(), exception);
        }
    }

    private static MemoryConfig buildMemoryConfig(ConfigDocument.MemoryDocument document) {
        MemoryConfig defaults = MemoryConfig.defaults();
        try {
            return new MemoryConfig(
                    document == null || document.enabled() == null ? defaults.enabled() : document.enabled(),
                    document == null || document.autoExtract() == null
                            ? defaults.autoExtract() : document.autoExtract(),
                    document == null || document.userScopeEnabled() == null
                            ? defaults.userScopeEnabled() : document.userScopeEnabled(),
                    document == null || document.projectScopeEnabled() == null
                            ? defaults.projectScopeEnabled() : document.projectScopeEnabled(),
                    document == null || document.maxEntriesPerScope() == null
                            ? defaults.maxEntriesPerScope() : document.maxEntriesPerScope(),
                    document == null || document.maxEntryChars() == null
                            ? defaults.maxEntryChars() : document.maxEntryChars(),
                    document == null || document.maxFileBytes() == null
                            ? defaults.maxFileBytes() : document.maxFileBytes(),
                    document == null || document.extractionOutputTokens() == null
                            ? defaults.extractionOutputTokens() : document.extractionOutputTokens());
        } catch (IllegalArgumentException exception) {
            throw new ConfigException("配置无效：" + exception.getMessage(), exception);
        }
    }

    private static double mergeThreshold(
            String environmentValue,
            Double yamlValue,
            double defaultValue,
            String name) {
        if (environmentValue != null && !environmentValue.isBlank()) {
            return parseThreshold(environmentValue, name);
        }
        if (yamlValue == null) {
            return defaultValue;
        }
        if (!Double.isFinite(yamlValue) || yamlValue <= 0d || yamlValue >= 1d) {
            throw new ConfigException("config.yaml 配置项 context.auto-compact-threshold 必须在 0 和 1 之间");
        }
        return yamlValue;
    }

    private static double parseThreshold(String value, String name) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (!Double.isFinite(parsed) || parsed <= 0d || parsed >= 1d) {
                throw new NumberFormatException("out of range");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new ConfigException("配置项 " + name + " 必须是 0 和 1 之间的有限小数", exception);
        }
    }

    private String resolveYamlValue(
            String environmentOverride,
            String yamlValue,
            Map<String, String> environment,
            Consumer<String> secretRegistrar,
            String configPath) {
        if (environmentOverride != null && !environmentOverride.isBlank()) {
            return environmentOverride.trim();
        }
        if (yamlValue == null || yamlValue.isBlank()) {
            return null;
        }
        try {
            return placeholderResolver.expand(yamlValue.trim(), environment, secretRegistrar);
        } catch (MissingEnvironmentVariableException exception) {
            throw new ConfigException(
                    "配置项 " + configPath + " 缺少环境变量 " + exception.variableName(),
                    exception);
        }
    }

    private static String yamlName(String environmentName) {
        return switch (environmentName) {
            case "IMIO_CONNECT_TIMEOUT_SECONDS" -> "connect-timeout-seconds";
            case "IMIO_REQUEST_TIMEOUT_SECONDS" -> "request-timeout-seconds";
            case "IMIO_MAX_OUTPUT_TOKENS" -> "max-output-tokens";
            case "IMIO_THINKING_BUDGET_TOKENS" -> "thinking.budget-tokens";
            case "IMIO_AGENT_MAX_ITERATIONS" -> "agent.max-iterations";
            case "IMIO_AGENT_TIMEOUT_SECONDS" -> "agent.timeout-seconds";
            case "IMIO_AGENT_MAX_PARALLEL_TOOLS" -> "agent.max-parallel-tools";
            case "IMIO_CONTEXT_WINDOW_TOKENS" -> "context.window-tokens";
            default -> environmentName;
        };
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String apiKeyName(Provider provider) {
        return switch (provider) {
            case OPENAI -> "OPENAI_API_KEY";
            case ANTHROPIC -> "ANTHROPIC_API_KEY";
            case DEEPSEEK -> "DEEPSEEK_API_KEY";
        };
    }

    private static String baseUrlName(Provider provider) {
        return switch (provider) {
            case OPENAI -> "OPENAI_BASE_URL";
            case ANTHROPIC -> "ANTHROPIC_BASE_URL";
            case DEEPSEEK -> "DEEPSEEK_BASE_URL";
        };
    }

    private static URI defaultBaseUri(Provider provider) {
        return switch (provider) {
            case OPENAI -> URI.create("https://api.openai.com");
            case ANTHROPIC -> URI.create("https://api.anthropic.com");
            case DEEPSEEK -> URI.create("https://api.deepseek.com");
        };
    }

    private static URI stripTrailingSlash(URI uri) {
        String text = uri.toString();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return URI.create(text);
    }

    private static boolean hasLegacyMcpConfig(Path workspace, Path userHome) {
        return exists(workspace.resolve(".imiocode").resolve("mcp.local.yaml"))
                || exists(workspace.resolve(".imiocode").resolve("mcp.yaml"))
                || exists(userHome.resolve(".imiocode").resolve("mcp.yaml"));
    }

    private static boolean hasLegacyPermissionConfig(Path workspace, Path userHome) {
        return exists(workspace.resolve(".imiocode").resolve("permissions.local.yaml"))
                || exists(workspace.resolve(".imiocode").resolve("permissions.yaml"))
                || exists(userHome.resolve(".imiocode").resolve("permissions.yaml"));
    }

    private static boolean exists(Path path) {
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }
}
