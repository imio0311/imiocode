package io.imiocode.mcp.config;

import io.imiocode.tool.SecretRedactor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 展开 MCP env/headers 中的启动环境占位符。 */
public final class McpEnvironmentResolver {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");

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
        } catch (MissingVariableException exception) {
            return Resolution.failure(exception.variableName);
        }
    }

    private static Map<String, String> expandMap(
            Map<String, String> values,
            Map<String, String> environment,
            SecretRedactor redactor) {
        Map<String, String> expanded = new LinkedHashMap<>();
        values.forEach((key, value) -> expanded.put(
                Objects.requireNonNull(key, "配置名称"),
                expand(Objects.requireNonNull(value, "配置值"), environment, redactor)));
        return Map.copyOf(expanded);
    }

    private static String expand(
            String value,
            Map<String, String> environment,
            SecretRedactor redactor) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String replacement = environment.get(name);
            if (replacement == null) {
                throw new MissingVariableException(name);
            }
            redactor.registerSecret(replacement);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
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

    private static final class MissingVariableException extends RuntimeException {
        private final String variableName;

        private MissingVariableException(String variableName) {
            this.variableName = variableName;
        }
    }
}
