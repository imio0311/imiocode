package io.imiocode.team.backend;

import io.imiocode.team.model.TeamBackend;
import java.time.Duration;

/**
 * 团队成员执行载体的统一生命周期接口。
 *
 * <p>后端句柄必须足以支持后续唤醒和停止；显式后端失败时由上层决定是否回退，接口自身不静默切换实现。</p>
 */
public interface TeammateBackend extends AutoCloseable {
    TeamBackend kind();
    BackendAvailability probe(Duration timeout);
    BackendHandle start(TeammateLaunchRequest request);
    void wake(BackendHandle handle);
    void stop(BackendHandle handle,Duration timeout);
    @Override default void close() { }
}
