package io.imiocode.conversation;

public sealed interface MessagePart permits TextPart, ToolCallPart, ToolResultPart {
}
