package io.imiocode.skill;

import io.imiocode.agent.Agent;
import io.imiocode.agent.AgentEventListener;
import io.imiocode.agent.AgentRequest;
import io.imiocode.agent.AgentResult;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessageRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicReference;

/** 使用独立 Agent 实例执行 fork Skill，并只返回最终文本。 */
public final class DefaultSkillForkRunner implements SkillForkRunner {
    private static final int RECENT_MESSAGE_LIMIT = 12;
    private final Supplier<Agent> agentFactory;
    private final AtomicReference<Agent> activeAgent = new AtomicReference<>();

    public DefaultSkillForkRunner(Supplier<Agent> agentFactory) {
        this.agentFactory = Objects.requireNonNull(agentFactory, "agentFactory");
    }

    @Override
    public String run(SkillInvocation invocation, List<ChatMessage> parentHistory,
                      AgentEventListener listener) {
        List<ChatMessage> selected = selectHistory(
                parentHistory, invocation.skill().metadata().history());
        String request = "执行已激活的 Skill /" + invocation.skill().metadata().name()
                + "，严格遵循其 SOP 完成任务。"
                + (invocation.arguments().isBlank() ? "" : "\n用户参数：" + invocation.arguments());
        Agent agent = agentFactory.get();
        if (!activeAgent.compareAndSet(null, agent)) {
            agent.close();
            throw new SkillException("已有 fork Skill 正在执行");
        }
        try {
            AgentResult result = agent.run(new AgentRequest(
                    selected,
                    new ChatMessage(MessageRole.USER, request),
                    List.of(),
                    Optional.of(invocation)), listener);
            if (!result.completed()) {
                throw new SkillException("fork Skill 未正常完成: " + result.stopReason());
            }
            return result.finalResponse().orElseThrow().text();
        } finally {
            activeAgent.compareAndSet(agent, null);
            agent.close();
        }
    }

    @Override public void cancelActive() {
        Agent agent = activeAgent.get();
        if (agent != null) agent.cancelActive();
    }

    @Override public boolean isActive() { return activeAgent.get() != null; }

    public static List<ChatMessage> selectHistory(List<ChatMessage> history, SkillHistoryMode mode) {
        List<ChatMessage> source = List.copyOf(Objects.requireNonNullElse(history, List.of()));
        return switch (Objects.requireNonNullElse(mode, SkillHistoryMode.RECENT)) {
            case FULL -> source;
            case NONE -> List.of();
            case RECENT -> recent(source);
        };
    }

    private static List<ChatMessage> recent(List<ChatMessage> history) {
        if (history.isEmpty()) return List.of();
        int start = Math.max(0, history.size() - RECENT_MESSAGE_LIMIT);
        while (start < history.size() && history.get(start).role() != MessageRole.USER) start++;
        if (start >= history.size()) return List.of();
        return List.copyOf(new ArrayList<>(history.subList(start, history.size())));
    }
}
