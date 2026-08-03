package io.imiocode.session.record;

public record SessionHeaderRecord(String type, int schemaVersion, String sessionId,
                                  String workspaceIdentity, String createdAt) {
    public SessionHeaderRecord(String sessionId, String workspaceIdentity, String createdAt) {
        this("session_header", 1, sessionId, workspaceIdentity, createdAt);
    }
}
