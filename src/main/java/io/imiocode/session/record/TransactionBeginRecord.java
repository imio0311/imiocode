package io.imiocode.session.record;

public record TransactionBeginRecord(String type, String transactionId,
                                     TransactionMode mode, int baseMessageCount) {
    public TransactionBeginRecord(String transactionId, TransactionMode mode, int baseMessageCount) {
        this("transaction_begin", transactionId, mode, baseMessageCount);
    }
}
