package io.imiocode.mcp.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.imiocode.tool.SecretRedactor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 加载、合并、校验并展开三层 MCP 配置。 */
public final class McpConfigLoader {
    private final ObjectMapper mapper;
    private final McpEnvironmentResolver resolver;

    public McpConfigLoader() {
        this(JsonMapper.builder(new YAMLFactory())
                .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                .build(), new McpEnvironmentResolver());
    }

    McpConfigLoader(ObjectMapper mapper, McpEnvironmentResolver resolver) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public McpConfigLoadResult load(
            Path workspace,
            Path userHome,
            Map<String, String> startupEnvironment,
            SecretRedactor redactor) {
        Objects.requireNonNull(workspace, "workspace");
        Objects.requireNonNull(userHome, "userHome");
        Objects.requireNonNull(startupEnvironment, "startupEnvironment");
        Objects.requireNonNull(redactor, "redactor");

        List<McpConfigError> errors = new ArrayList<>();
        Map<String, McpServerConfig> merged = new LinkedHashMap<>();
        List<Path> paths = List.of(
                workspace.resolve(".imiocode").resolve("mcp.local.yaml"),
                workspace.resolve(".imiocode").resolve("mcp.yaml"),
                userHome.resolve(".imiocode").resolve("mcp.yaml"));
        for (Path path : paths) {
            loadLayer(path.toAbsolutePath().normalize(), merged, errors);
        }

        Map<String, ResolvedMcpServerConfig> resolved = new LinkedHashMap<>();
        merged.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    McpServerConfig config = entry.getValue();
                    if (!config.enabled()) {
                        return;
                    }
                    McpEnvironmentResolver.Resolution resolution =
                            resolver.resolve(config, startupEnvironment, redactor);
                    if (resolution.success()) {
                        resolved.put(entry.getKey(), resolution.config());
                    } else {
                        errors.add(new McpConfigError(
                                config.source(),
                                config.name(),
                                "missing_environment",
                                "缺少环境变量: " + resolution.missingVariable()));
                    }
                });
        return new McpConfigLoadResult(resolved, errors);
    }

    private void loadLayer(
            Path path,
            Map<String, McpServerConfig> merged,
            List<McpConfigError> errors) {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            errors.add(new McpConfigError(path, "", "unsafe_file", "MCP 配置必须是普通文件且不能是符号链接"));
            return;
        }
        try {
            JsonNode root = mapper.readTree(path.toFile());
            if (root == null || root.isNull()) {
                return;
            }
            JsonNode servers = root.get("servers");
            if (servers == null || servers.isNull()) {
                return;
            }
            if (!servers.isObject()) {
                errors.add(new McpConfigError(path, "", "invalid_document", "MCP 配置的 servers 必须是对象"));
                return;
            }
            servers.fields().forEachRemaining(entry -> {
                try {
                    McpConfigDocument.ServerDocument document =
                            mapper.treeToValue(entry.getValue(), McpConfigDocument.ServerDocument.class);
                    merged.put(entry.getKey(), toConfig(entry.getKey(), document, path));
                } catch (JsonProcessingException | IllegalArgumentException exception) {
                    errors.add(new McpConfigError(
                            path, entry.getKey(), "invalid_server", "MCP Server 配置无效"));
                }
            });
        } catch (JsonProcessingException exception) {
            errors.add(new McpConfigError(path, "", "invalid_yaml", "MCP YAML 格式错误"));
        } catch (IOException exception) {
            errors.add(new McpConfigError(path, "", "read_failed", "无法读取 MCP 配置"));
        }
    }

    private static McpServerConfig toConfig(
            String name,
            McpConfigDocument.ServerDocument document,
            Path source) {
        if (document == null) {
            throw new IllegalArgumentException("Server 配置不能为空");
        }
        boolean enabled = document.enabled() == null || document.enabled();
        McpTransportType transport = McpTransportType.parse(document.transport());
        String command = document.command() == null ? "" : document.command().trim();
        URI url = parseUri(document.url());
        if (transport == McpTransportType.STDIO) {
            if (command.isBlank() || url != null) {
                throw new IllegalArgumentException("stdio 配置无效");
            }
        } else {
            if (!command.isBlank() || url == null) {
                throw new IllegalArgumentException("HTTP 配置无效");
            }
            validateHttpUri(url);
        }
        Duration initializationTimeout = seconds(
                document.initializationTimeoutSeconds(),
                McpServerConfig.DEFAULT_INITIALIZATION_TIMEOUT);
        Duration callTimeout = seconds(
                document.callTimeoutSeconds(),
                McpServerConfig.DEFAULT_CALL_TIMEOUT);
        return new McpServerConfig(
                name,
                enabled,
                transport,
                command,
                document.args(),
                url,
                document.env(),
                document.headers(),
                initializationTimeout,
                callTimeout,
                source);
    }

    private static URI parseUri(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new URI(value.trim());
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("URL 无效", exception);
        }
    }

    private static void validateHttpUri(URI uri) {
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL 不能包含凭据");
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            throw new IllegalArgumentException("URL 必须是绝对地址");
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return;
        }
        if ("http".equalsIgnoreCase(scheme) && isLoopback(host)) {
            return;
        }
        throw new IllegalArgumentException("远程 MCP 必须使用 HTTPS");
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }

    private static Duration seconds(Long seconds, Duration fallback) {
        if (seconds == null) {
            return fallback;
        }
        if (seconds <= 0) {
            throw new IllegalArgumentException("超时必须为正数");
        }
        return Duration.ofSeconds(seconds);
    }
}
