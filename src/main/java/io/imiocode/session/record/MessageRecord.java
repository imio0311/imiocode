package io.imiocode.session.record;

public record MessageRecord(String type, String transactionId, int sequence, StoredMessage message) {
    public MessageRecord(String transactionId, int sequence, StoredMessage message) {
        this("message", transactionId, sequence, message);
    }
}
