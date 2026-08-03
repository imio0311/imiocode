package io.imiocode.session;

import io.imiocode.config.SessionsConfig;
import io.imiocode.conversation.ChatMessage;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

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
        List<SessionSummary> summaries = store.list().stream()
                .filter(item -> !item.id().equals(current))
                .sorted(Comparator.comparing(SessionSummary::updatedAt)).toList();
        Instant cutoff = config.retentionDays() == 0 ? Instant.MIN
                : clock.instant().minus(config.retentionDays(), ChronoUnit.DAYS);
        int excess = config.maxSessions() == 0 ? 0 : Math.max(0, summaries.size() + 1 - config.maxSessions());
        for (int i = 0; i < summaries.size(); i++) {
            SessionSummary item = summaries.get(i);
            if (i < excess || item.updatedAt().isBefore(cutoff)) store.delete(item.id());
        }
    }
}
