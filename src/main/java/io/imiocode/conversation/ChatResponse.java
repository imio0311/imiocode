package io.imiocode.conversation;

public record ChatResponse(String content) {
    public ChatResponse {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("模型回复不能为空");
        }
    }
}
