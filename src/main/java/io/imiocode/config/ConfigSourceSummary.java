package io.imiocode.config;

import java.util.Objects;

/** 基础、MCP 与权限配置的安全来源摘要。 */
public record ConfigSourceSummary(
        ConfigSource app,
        ConfigSource mcp,
        ConfigSource permissions) {

    public ConfigSourceSummary {
        Objects.requireNonNull(app, "app");
        Objects.requireNonNull(mcp, "mcp");
        Objects.requireNonNull(permissions, "permissions");
    }
}
