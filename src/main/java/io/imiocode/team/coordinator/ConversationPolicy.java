package io.imiocode.team.coordinator;

import io.imiocode.conversation.SystemReminder;
import io.imiocode.conversation.ConversationRuntimePolicy;
import io.imiocode.tool.ToolSelection;
import java.util.*;

/** 会话每轮读取的动态工具选择与提醒。 */
public final class ConversationPolicy implements ConversationRuntimePolicy {
    private final CoordinatorModeController coordinator;
    public ConversationPolicy(CoordinatorModeController coordinator){this.coordinator=Objects.requireNonNull(coordinator);}
    public Optional<ToolSelection> selection(){CoordinatorSnapshot s=coordinator.snapshot();return s.active()?Optional.of(s.selection()):Optional.empty();}
    public List<SystemReminder> reminders(){return coordinator.reminder().map(List::of).orElseGet(List::of);}
    public static ConversationPolicy inactive(){return new ConversationPolicy(new CoordinatorModeController(false,Map.of()));}
}
