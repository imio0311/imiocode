package io.imiocode.config;

/** 项目会话存档和自动清理配置；0 表示不启用对应清理条件。 */
public record SessionsConfig(boolean enabled, int retentionDays, int maxSessions) {
    public static final int HARD_MAX_RETENTION_DAYS = 36_500;
    public static final int HARD_MAX_SESSIONS = 100_000;

    public SessionsConfig {
        if (retentionDays < 0 || retentionDays > HARD_MAX_RETENTION_DAYS) {
            throw new IllegalArgumentException("sessions.retention-days 必须在 0 和 36500 之间");
        }
        if (maxSessions < 0 || maxSessions > HARD_MAX_SESSIONS) {
            throw new IllegalArgumentException("sessions.max-sessions 必须在 0 和 100000 之间");
        }
    }

    public static SessionsConfig defaults() {
        return new SessionsConfig(true, 0, 0);
    }
}
