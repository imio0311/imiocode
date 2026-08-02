package io.imiocode.mcp.config;

import io.imiocode.config.EnvironmentPlaceholderResolver;
import io.imiocode.config.MissingEnvironmentVariableException;
import io.imiocode.tool.SecretRedactor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 展开 MCP env/headers 中的启动环境占位符。 */
public final class McpEnvironmentResolver {
    private final EnvironmentPlaceholderResolver placeholderResolver;

    public McpEnvironmentResolver() {
        this(new EnvironmentPlaceholderResolver());
    }

    McpEnvironmentResolver(EnvironmentPlaceholderResolver placeholderResolver) {
        this.placeholderResolver = Objects.requireNonNull(placeholderResolver, "placeholderResolver");
    }

    public Resolution resolve(
            McpServerConfig config,
            Map<String, String> startupEnvironment,
            SecretRedactor redactor) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(startupEnvironment, "startupEnvironment");
        Objects.requireNonNull(redactor, "redactor");
        try {
            Map<String, String> env = expandMap(config.env(), startupEnvironment, redactor);
            Map<String, String> headers = expandMap(config.headers(), startupEnvironment, redactor);
            return Resolution.success(new ResolvedMcpServerConfig(
                    config.name(),
                    config.transport(),
                    config.command(),
                    config.args(),
                    config.url(),
                    env,
                    headers,
                    config.initializationTimeout(),
                    config.callTimeout(),
                    config.source()));
        } catch (MissingEnvironmentVariableException exception) {
            return Resolution.failure(exception.variableName());
        }
    }

    private Map<String, String> expandMap(
            Map<String, String> values,
            Map<String, String> environment,
            SecretRedactor redactor) {
        Map<String, String> expanded = new LinkedHashMap<>();
        values.forEach((key, value) -> expanded.put(
                Objects.requireNonNull(key, "配置名称"),
                placeholderResolver.expand(
                        Objects.requireNonNull(value, "配置值"),
                        environment,
                        redactor::registerSecret)));
        return Map.copyOf(expanded);
    }

    public record Resolution(ResolvedMcpServerConfig config, String missingVariable) {
        public static Resolution success(ResolvedMcpServerConfig config) {
            return new Resolution(Objects.requireNonNull(config, "config"), "");
        }

        public static Resolution failure(String variable) {
            return new Resolution(null, Objects.requireNonNull(variable, "variable"));
        }

        public boolean success() {
            return config != null;
        }
    }
}
