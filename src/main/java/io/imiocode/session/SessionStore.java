package io.imiocode.session;

import java.util.List;

/**
 * 会话快照的持久化边界。
 *
 * <p>{@link #appendCommit} 必须把一次成功轮次作为完整事务追加；实现不得向调用方暴露未提交的半轮历史。</p>
 */
public interface SessionStore {
    SessionSnapshot create();
    SessionSnapshot appendCommit(SessionSnapshot before, List<io.imiocode.conversation.ChatMessage> afterHistory);
    SessionLoadResult load(SessionId id);
    List<SessionSummary> list();
    void delete(SessionId id);
}
