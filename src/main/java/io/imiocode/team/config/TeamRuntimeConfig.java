package io.imiocode.team.config;

import io.imiocode.team.model.TeamBackend;

import java.time.Duration;
import java.util.Objects;

/** Agent Team 的容量、安全上限与功能开关。 */
public record TeamRuntimeConfig(TeamBackend preferredBackend, boolean coordinatorEnabled,
        int maxTeams, int maxMembersPerTeam, int maxTasksPerTeam, int maxMessagesPerMailbox,
        int maxMessageChars, int maxTranscriptBytes, Duration probeTimeout, Duration shutdownTimeout) {
    public TeamRuntimeConfig {
        Objects.requireNonNull(preferredBackend); Objects.requireNonNull(probeTimeout); Objects.requireNonNull(shutdownTimeout);
        if (maxTeams <= 0 || maxMembersPerTeam <= 0 || maxTasksPerTeam <= 0 || maxMessagesPerMailbox <= 0
                || maxMessageChars <= 0 || maxTranscriptBytes <= 0) throw new IllegalArgumentException("团队容量上限必须为正数");
        if (maxTeams > 64 || maxMembersPerTeam > 64 || maxTasksPerTeam > 10_000
                || maxMessagesPerMailbox > 100_000 || maxMessageChars > 1_000_000
                || maxTranscriptBytes > 256 * 1024 * 1024) {
            throw new IllegalArgumentException("团队容量配置超过安全上限");
        }
        if (probeTimeout.isZero() || probeTimeout.isNegative() || shutdownTimeout.isZero() || shutdownTimeout.isNegative())
            throw new IllegalArgumentException("团队超时必须为正数");
        if (probeTimeout.compareTo(Duration.ofSeconds(30)) > 0
                || shutdownTimeout.compareTo(Duration.ofMinutes(2)) > 0) {
            throw new IllegalArgumentException("团队超时配置超过安全上限");
        }
    }
    public static TeamRuntimeConfig defaults() {
        return new TeamRuntimeConfig(TeamBackend.AUTO, false, 8, 8, 256, 2048, 32_000,
                8 * 1024 * 1024, Duration.ofSeconds(2), Duration.ofSeconds(10));
    }
}
