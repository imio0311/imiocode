package io.imiocode.agent;

import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanModePromptTest {

    @Test
    void emitsFullReminderOnRoundsOneSixAndEleven() {
        SystemReminder first = PlanModePrompt.reminder(AgentMode.PLAN, 1).orElseThrow();
        SystemReminder sixth = PlanModePrompt.reminder(AgentMode.PLAN, 6).orElseThrow();
        SystemReminder eleventh = PlanModePrompt.reminder(AgentMode.PLAN, 11).orElseThrow();

        assertEquals(ReminderScope.ROUND, first.scope());
        assertEquals(first, sixth);
        assertEquals(first, eleventh);
        assertTrue(first.content().contains("当前处于 Plan Mode"));
        assertTrue(first.content().contains("最终输出"));
    }

    @Test
    void emitsConciseReminderBetweenFullRounds() {
        SystemReminder first = PlanModePrompt.reminder(AgentMode.PLAN, 1).orElseThrow();
        for (int iteration = 2; iteration <= 5; iteration++) {
            SystemReminder reminder =
                    PlanModePrompt.reminder(AgentMode.PLAN, iteration).orElseThrow();
            assertEquals(ReminderScope.ROUND, reminder.scope());
            assertFalse(reminder.equals(first));
            assertTrue(reminder.content().contains("只读"));
        }
    }

    @Test
    void emitsNothingInDoModeAndRejectsInvalidIteration() {
        assertTrue(PlanModePrompt.reminder(AgentMode.DO, 1).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> PlanModePrompt.reminder(AgentMode.PLAN, 0));
    }
}
