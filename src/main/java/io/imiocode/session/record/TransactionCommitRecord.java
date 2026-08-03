package io.imiocode.session.record;

public record TransactionCommitRecord(String type, String transactionId, int messageCount,
                                      long commitNumber, String sha256, String committedAt) {
    public TransactionCommitRecord(String transactionId, int messageCount, long commitNumber,
                                   String sha256, String committedAt) {
        this("transaction_commit", transactionId, messageCount, commitNumber, sha256, committedAt);
    }
}
