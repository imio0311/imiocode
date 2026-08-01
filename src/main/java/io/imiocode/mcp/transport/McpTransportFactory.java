package io.imiocode.mcp.transport;

import io.imiocode.mcp.config.McpTransportType;
import io.imiocode.mcp.config.ResolvedMcpServerConfig;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;

import java.util.Objects;

/** 根据 Server 配置创建对应的标准 Transport。 */
public class McpTransportFactory {
    private final JsonRpcCodec codec;

    public McpTransportFactory(JsonRpcCodec codec) {
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public McpTransport create(ResolvedMcpServerConfig config) {
        Objects.requireNonNull(config, "config");
        return config.transport() == McpTransportType.STDIO
                ? new StdioMcpTransport(config, codec)
                : new StreamableHttpMcpTransport(config, codec);
    }
}
