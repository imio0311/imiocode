package io.imiocode.team.backend;

import io.imiocode.team.model.TeamBackend;
import java.time.Duration;

public interface TeammateBackend extends AutoCloseable {
    TeamBackend kind();
    BackendAvailability probe(Duration timeout);
    BackendHandle start(TeammateLaunchRequest request);
    void wake(BackendHandle handle);
    void stop(BackendHandle handle,Duration timeout);
    @Override default void close() { }
}
