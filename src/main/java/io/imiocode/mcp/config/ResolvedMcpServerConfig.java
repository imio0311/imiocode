package io.imiocode.mcp.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 已展开占位符的配置；字符串表示不会泄露 env 或 headers。 */
public final class ResolvedMcpServerConfig {
    private final String name;
    private final McpTransportType transport;
    private final String command;
    private final List<String> args;
    private final URI url;
    private final Map<String, String> env;
    private final Map<String, String> headers;
    private final Duration initializationTimeout;
    private final Duration callTimeout;
    private final Path source;

    public ResolvedMcpServerConfig(
            String name,
            McpTransportType transport,
            String command,
            List<String> args,
            URI url,
            Map<String, String> env,
            Map<String, String> headers,
            Duration initializationTimeout,
            Duration callTimeout,
            Path source) {
        this.name = Objects.requireNonNull(name, "name");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.command = Objects.requireNonNullElse(command, "");
        this.args = List.copyOf(Objects.requireNonNullElse(args, List.of()));
        this.url = url;
        this.env = Map.copyOf(Objects.requireNonNullElse(env, Map.of()));
        this.headers = Map.copyOf(Objects.requireNonNullElse(headers, Map.of()));
        this.initializationTimeout = Objects.requireNonNull(initializationTimeout, "initializationTimeout");
        this.callTimeout = Objects.requireNonNull(callTimeout, "callTimeout");
        this.source = Objects.requireNonNull(source, "source");
    }

    public String name() {
        return name;
    }

    public McpTransportType transport() {
        return transport;
    }

    public String command() {
        return command;
    }

    public List<String> args() {
        return args;
    }

    public URI url() {
        return url;
    }

    public Map<String, String> env() {
        return env;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public Duration initializationTimeout() {
        return initializationTimeout;
    }

    public Duration callTimeout() {
        return callTimeout;
    }

    public Path source() {
        return source;
    }

    @Override
    public String toString() {
        return "ResolvedMcpServerConfig[name=" + name
                + ", transport=" + transport
                + ", command=" + command
                + ", args=" + args
                + ", url=" + url
                + ", env=***, headers=***]";
    }
}
