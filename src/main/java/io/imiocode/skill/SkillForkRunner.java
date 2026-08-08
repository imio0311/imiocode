package io.imiocode.skill;

import io.imiocode.agent.AgentEventListener;
import io.imiocode.conversation.ChatMessage;

import java.util.List;

/** 由应用层提供的隔离 Agent 执行入口。 */
@FunctionalInterface
public interface SkillForkRunner {
    String run(SkillInvocation invocation, List<ChatMessage> parentHistory,
               AgentEventListener listener);

    default void cancelActive() { }

    default boolean isActive() { return false; }
}
