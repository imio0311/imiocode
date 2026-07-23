package io.imiocode.conversation;

public enum MessageRole {
    USER("user"),
    ASSISTANT("assistant");

    private final String apiValue;

    MessageRole(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
