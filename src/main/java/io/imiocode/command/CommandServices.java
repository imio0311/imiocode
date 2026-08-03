package io.imiocode.command;

import io.imiocode.agent.AgentMode;
import io.imiocode.context.CompactReport;
import io.imiocode.memory.MemoryDocument;
import io.imiocode.memory.MemoryEntry;
import io.imiocode.memory.MemoryScope;
import io.imiocode.session.SessionId;
import io.imiocode.session.SessionLoadResult;
import io.imiocode.session.SessionSummary;
import java.util.List;
import java.util.Optional;

public interface CommandServices {
    AgentMode mode();
    void switchMode(AgentMode mode);
    CompactReport compact();
    SessionSummary currentSession();
    List<SessionSummary> listSessions();
    SessionSummary newSession();
    SessionLoadResult resumeSession(SessionId id);
    void deleteSession(SessionId id);
    boolean sessionsEnabled();
    List<MemoryDocument> listMemories(Optional<MemoryScope> scope);
    MemoryEntry addMemory(MemoryScope scope, String content);
    MemoryEntry editMemory(MemoryScope scope, String id, String content);
    void forgetMemory(MemoryScope scope, String id);
    boolean memoryEnabled();
}
