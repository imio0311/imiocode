package io.imiocode.mcp.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** YAML 单层文档的 Jackson 映射类型。 */
public record McpConfigDocument(Map<String, ServerDocument> servers) {
    public McpConfigDocument {
        servers = Map.copyOf(servers == null ? Map.of() : servers);
    }

    public record ServerDocument(
            Boolean enabled,
            String transport,
            String command,
            List<String> args,
            String url,
            Map<String, String> env,
            Map<String, String> headers,
            @JsonProperty("initialization-timeout-seconds")
            Long initializationTimeoutSeconds,
            @JsonProperty("call-timeout-seconds")
            Long callTimeoutSeconds) {
    }
}
