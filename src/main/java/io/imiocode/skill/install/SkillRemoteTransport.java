package io.imiocode.skill.install;

import java.net.URI;

/** 执行可取消、受下载预算约束的远程 Skill HTTP 读取。 */
public interface SkillRemoteTransport extends AutoCloseable {
    RemoteResponse get(URI uri, SkillDownloadBudget budget);
    void cancel();
    @Override default void close() { cancel(); }
}
