package io.imiocode.persistence;

import io.imiocode.session.SessionId;
import java.nio.file.Path;

public sealed interface PersistenceEvent {
    record SessionSaved(SessionId id, long commitCount) implements PersistenceEvent {}
    record SessionRestored(SessionId id, int messageCount) implements PersistenceEvent {}
    record SessionTailRecovered(SessionId id, Path quarantinedTail) implements PersistenceEvent {}
    record MemoryUpdated(int added, int updated, int skipped) implements PersistenceEvent {}
    record Warning(String safeMessage) implements PersistenceEvent {}
}
