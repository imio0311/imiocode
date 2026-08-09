package io.imiocode.agent;

import io.imiocode.conversation.ChatMessage;

import java.util.List;
import java.util.Optional;

/** 将当前轮完整消息快照传给工具线程，供 agent Fork 保留父上下文。 */
public final class AgentHistoryContext {
    private static final InheritableThreadLocal<List<ChatMessage>> CURRENT = new InheritableThreadLocal<>();
    private AgentHistoryContext() { }
    public static void set(List<ChatMessage> messages) { CURRENT.set(List.copyOf(messages)); }
    public static Optional<List<ChatMessage>> current() { return Optional.ofNullable(CURRENT.get()); }
    public static void clear() { CURRENT.remove(); }
}
