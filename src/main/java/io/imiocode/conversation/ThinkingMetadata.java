package io.imiocode.conversation;

public sealed interface ThinkingMetadata permits AnthropicThinkingMetadata,
        OpenAiReasoningMetadata, DeepSeekReasoningMetadata {
}
