package io.imiocode.conversation;

public sealed interface MessagePart permits TextPart, ThinkingPart, ToolCallPart, ToolResultPart {
}
