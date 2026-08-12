package io.imiocode.session;

import io.imiocode.config.SessionsConfig;
import io.imiocode.conversation.ChatMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * 提供会话存储的应用层入口，并在创建或切换会话后执行保留策略。
 *
 * <p>事务完整性与损坏恢复由 {@link SessionStore} 负责；本类型只编排 CRUD 和按时间、数量清理。</p>
 */
public final class SessionManager {
    private final SessionStore store;
    private final SessionsConfig config;
    private final Clock clock;

    public SessionManager(SessionStore store, SessionsConfig config, Clock clock) {
        this.store = store; this.config = config; this.clock = clock;
    }

    public SessionSnapshot createNew() { return store.create(); }
    public SessionLoadResult load(SessionId id) { return store.load(id); }
    public SessionSnapshot commit(SessionSnapshot before, List<ChatMessage> after) { return store.appendCommit(before, after); }
    public List<SessionSummary> list() { return store.list(); }
    public void delete(SessionId id) { store.delete(id); }

    public void applyRetention(SessionId current) {
        if (config.retentionDays() == 0 && config.maxSessions() == 0) return;
        // 当前会话必须保留；数量上限中的 +1 正是为当前会话预留的位置。
        List<SessionSummary> summaries = store.list().stream()
                .filter(item -> !item.id().equals(current))
                .sorted(Comparator.comparing(SessionSummary::updatedAt)).toList();
        Instant cutoff = config.retentionDays() == 0 ? Instant.MIN
                : clock.instant().minus(config.retentionDays(), ChronoUnit.DAYS);
        int excess = config.maxSessions() == 0 ? 0 : Math.max(0, summaries.size() + 1 - config.maxSessions());
        for (int i = 0; i < summaries.size(); i++) {
            SessionSummary item = summaries.get(i);
            // 列表按更新时间升序排列，先删除超出数量上限的最旧会话，再应用时间截止线。
            if (i < excess || item.updatedAt().isBefore(cutoff)) store.delete(item.id());
        }
    }
}
