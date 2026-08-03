package io.imiocode.session;

import java.nio.file.Path;
import java.util.Optional;

public record SessionLoadResult(SessionSnapshot snapshot, SessionRecoveryStatus status,
                                Optional<Path> quarantinedTail, Optional<String> warning) {
    public SessionLoadResult {
        quarantinedTail = quarantinedTail == null ? Optional.empty() : quarantinedTail;
        warning = warning == null ? Optional.empty() : warning;
    }

    public static SessionLoadResult clean(SessionSnapshot snapshot) {
        return new SessionLoadResult(snapshot, SessionRecoveryStatus.CLEAN, Optional.empty(), Optional.empty());
    }
}
