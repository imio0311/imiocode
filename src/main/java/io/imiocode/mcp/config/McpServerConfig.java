package io.imiocode.mcp.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 合并后、环境变量展开前的单个 MCP Server 配置。 */
public record McpServerConfig(
        String name,
        boolean enabled,
        McpTransportType transport,
        String command,
        List<String> args,
        URI url,
        Map<String, String> env,
        Map<String, String> headers,
        Duration initializationTimeout,
        Duration callTimeout,
        Path source) {

    public static final Duration DEFAULT_INITIALIZATION_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration DEFAULT_CALL_TIMEOUT = Duration.ofSeconds(120);

    public McpServerConfig {
        name = requireText(name, "name");
        transport = Objects.requireNonNull(transport, "transport");
        command = command == null ? "" : command.trim();
        args = List.copyOf(Objects.requireNonNullElse(args, List.of()));
        env = Map.copyOf(Objects.requireNonNullElse(env, Map.of()));
        headers = Map.copyOf(Objects.requireNonNullElse(headers, Map.of()));
        initializationTimeout = positive(
                Objects.requireNonNullElse(initializationTimeout, DEFAULT_INITIALIZATION_TIMEOUT),
                "initializationTimeout");
        callTimeout = positive(
                Objects.requireNonNullElse(callTimeout, DEFAULT_CALL_TIMEOUT),
                "callTimeout");
        source = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
    }

    private static Duration positive(Duration value, String name) {
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " 必须为正数");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value.trim();
    }
}
