package io.imiocode.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class ConfigLoader {
    static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(120);
    static final int DEFAULT_MAX_OUTPUT_TOKENS = 4096;
    static final int DEFAULT_THINKING_BUDGET_TOKENS = 1024;
    private final YamlConfigLoader yamlConfigLoader;

    public ConfigLoader() {
        this(new YamlConfigLoader());
    }

    ConfigLoader(YamlConfigLoader yamlConfigLoader) {
        this.yamlConfigLoader = Objects.requireNonNull(yamlConfigLoader, "yamlConfigLoader");
    }

    public AppConfig load(Map<String, String> environment) {
        return load(Path.of("").toAbsolutePath(), environment);
    }

    public AppConfig load(Path workingDirectory, Map<String, String> environment) {
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Objects.requireNonNull(environment, "environment");
        ConfigDocument document = yamlConfigLoader.load(workingDirectory);

        Provider provider = Provider.parse(firstNonBlank(environment.get("IMIO_PROVIDER"), document.provider()));
        String model = required(firstNonBlank(environment.get("IMIO_MODEL"), document.model()), "IMIO_MODEL / model");
        ProviderConfig providerConfig = document.providerConfig(provider);
        String apiKeyName = apiKeyName(provider);
        String apiKey = required(
                firstNonBlank(environment.get(apiKeyName), providerConfig.apiKey()),
                apiKeyName + " / providers." + provider.configValue() + ".api-key");
        String baseUrlName = baseUrlName(provider);
        URI baseUri = parseUri(
                firstNonBlank(environment.get(baseUrlName), providerConfig.baseUrl()),
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

        try {
            return new AppConfig(
                    provider, model, apiKey, baseUri, connectTimeout, requestTimeout,
                    maxOutputTokens, thinking, agent);
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

    private static String yamlName(String environmentName) {
        return switch (environmentName) {
            case "IMIO_CONNECT_TIMEOUT_SECONDS" -> "connect-timeout-seconds";
            case "IMIO_REQUEST_TIMEOUT_SECONDS" -> "request-timeout-seconds";
            case "IMIO_MAX_OUTPUT_TOKENS" -> "max-output-tokens";
            case "IMIO_THINKING_BUDGET_TOKENS" -> "thinking.budget-tokens";
            case "IMIO_AGENT_MAX_ITERATIONS" -> "agent.max-iterations";
            case "IMIO_AGENT_TIMEOUT_SECONDS" -> "agent.timeout-seconds";
            case "IMIO_AGENT_MAX_PARALLEL_TOOLS" -> "agent.max-parallel-tools";
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
}
