package io.imiocode.skill.install;

import java.net.URI;

public interface SkillRemoteTransport extends AutoCloseable {
    RemoteResponse get(URI uri, SkillDownloadBudget budget);
    void cancel();
    @Override default void close() { cancel(); }
}
