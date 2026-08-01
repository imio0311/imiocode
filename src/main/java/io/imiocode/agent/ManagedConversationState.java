package io.imiocode.agent;

import io.imiocode.context.ContextResult;
import io.imiocode.conversation.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Agent 内部可回滚的历史状态。 */
public final class ManagedConversationState {
    private List<ChatMessage> committed;
    private List<ChatMessage> rollbackCommitted;
    private final List<ChatMessage> trajectory = new ArrayList<>();

    public ManagedConversationState(List<ChatMessage> committed, ChatMessage userMessage) {
        this.committed = List.copyOf(Objects.requireNonNull(committed, "committed"));
        this.rollbackCommitted = this.committed;
        trajectory.add(Objects.requireNonNull(userMessage, "userMessage"));
    }

    public void apply(ContextResult result) {
        Objects.requireNonNull(result, "result");
        rollbackCommitted = result.committedHistory();
        if (result.compacted()) {
            committed = result.workingMessages();
            trajectory.clear();
        } else {
            committed = result.committedHistory();
            trajectory.clear();
            trajectory.addAll(result.trajectory());
        }
    }

    public void append(ChatMessage message) { trajectory.add(Objects.requireNonNull(message, "message")); }
    public List<ChatMessage> committed() { return List.copyOf(committed); }
    public List<ChatMessage> rollbackCommitted() { return List.copyOf(rollbackCommitted); }
    public List<ChatMessage> trajectory() { return List.copyOf(trajectory); }
    public List<ChatMessage> workingMessages() {
        List<ChatMessage> result = new ArrayList<>(committed.size() + trajectory.size());
        result.addAll(committed);
        result.addAll(trajectory);
        return List.copyOf(result);
    }
}
