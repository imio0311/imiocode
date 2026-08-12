package io.imiocode.session;

import io.imiocode.session.record.MessageRecord;
import io.imiocode.session.record.TransactionBeginRecord;
import io.imiocode.session.record.TransactionCommitRecord;
import io.imiocode.session.record.TransactionMode;

import java.util.List;

/**
 * 校验一段 JSONL 会话事务的 ID、序号、消息计数和摘要是否形成完整提交。
 *
 * <p>任一条件不一致都视为存储损坏，不能把部分消息恢复到对话历史。</p>
 */
public final class SessionIntegrityValidator {
    public void validateTransaction(TransactionBeginRecord begin, List<MessageRecord> messages,
                                    TransactionCommitRecord commit, int currentSize, long expectedCommit,
                                    String actualSha) {
        if (begin.transactionId() == null || begin.transactionId().isBlank()
                || !begin.transactionId().equals(commit.transactionId())) {
            throw new SessionException("事务 ID 不一致");
        }
        if (begin.mode() == null || begin.baseMessageCount() != currentSize) {
            throw new SessionException("事务基础消息数不一致");
        }
        if (messages.isEmpty()) throw new SessionException("事务没有消息");
        for (int i = 0; i < messages.size(); i++) {
            MessageRecord message = messages.get(i);
            if (!begin.transactionId().equals(message.transactionId()) || message.sequence() != i) {
                throw new SessionException("事务消息序号不连续");
            }
        }
        int expectedSize = begin.mode() == TransactionMode.APPEND ? currentSize + messages.size() : messages.size();
        if (commit.messageCount() != expectedSize || commit.commitNumber() != expectedCommit) {
            throw new SessionException("事务提交计数不一致");
        }
        if (commit.sha256() == null || !commit.sha256().equals(actualSha)) {
            throw new SessionException("事务摘要校验失败");
        }
    }
}
