package io.imiocode.skill.install;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** 远程 Skill 安装的不可变安全预算。 */
public record SkillInstallConfig(
        Duration timeout,
        int maxFiles,
        long maxFileBytes,
        long maxTotalBytes,
        Set<String> allowedHosts
) {
    public static final Set<String> TRUSTED_HOSTS = Set.of(
            "skills.sh", "github.com", "raw.githubusercontent.com", "api.github.com");

    public SkillInstallConfig {
        timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("安装超时必须大于 0");
        if (maxFiles <= 0) throw new IllegalArgumentException("最大文件数必须大于 0");
        if (maxFileBytes <= 0) throw new IllegalArgumentException("单文件大小上限必须大于 0");
        if (maxTotalBytes < maxFileBytes) throw new IllegalArgumentException("总大小上限不能小于单文件上限");
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String host : Objects.requireNonNullElse(allowedHosts, Set.<String>of())) {
            if (host == null || host.isBlank()) throw new IllegalArgumentException("允许域名不能为空");
            String value = host.trim().toLowerCase(Locale.ROOT);
            if (!TRUSTED_HOSTS.contains(value)) throw new IllegalArgumentException("不受支持的 Skill 下载域名: " + value);
            normalized.add(value);
        }
        if (normalized.isEmpty()) throw new IllegalArgumentException("允许域名不能为空");
        allowedHosts = Set.copyOf(normalized);
    }

    public static SkillInstallConfig defaults() {
        return new SkillInstallConfig(Duration.ofSeconds(30), 64, 256L * 1024,
                2L * 1024 * 1024, TRUSTED_HOSTS);
    }
}
