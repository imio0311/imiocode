package io.imiocode.session;

import java.util.List;

public interface SessionStore {
    SessionSnapshot create();
    SessionSnapshot appendCommit(SessionSnapshot before, List<io.imiocode.conversation.ChatMessage> afterHistory);
    SessionLoadResult load(SessionId id);
    List<SessionSummary> list();
    void delete(SessionId id);
}
