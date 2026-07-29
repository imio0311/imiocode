package io.imiocode.prompt;

import java.util.Objects;

/** 稳定 Prompt 通道的厂商无关缓存意图。 */
public record CacheIntent(
        CacheDirective system,
        CacheDirective tools
) {
    public CacheIntent {
        system = Objects.requireNonNull(system, "system 缓存意图不能为空");
        tools = Objects.requireNonNull(tools, "tools 缓存意图不能为空");
    }

    public static CacheIntent stableChannels() {
        return new CacheIntent(CacheDirective.EPHEMERAL, CacheDirective.EPHEMERAL);
    }

    public static CacheIntent systemOnly() {
        return new CacheIntent(CacheDirective.EPHEMERAL, CacheDirective.NONE);
    }
}
