package io.imiocode.conversation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SystemReminderTest {

    @Test
    void defaultsToSessionScopeAndWrapsContent() {
        var reminder = new SystemReminder("遵循项目约定");

        assertEquals(ReminderScope.SESSION, reminder.scope());
        assertEquals("<system-reminder>\n遵循项目约定\n</system-reminder>",
                reminder.wrappedContent());
    }

    @Test
    void preservesExplicitScope() {
        var reminder = new SystemReminder(ReminderScope.ROUND, "本轮只读");

        assertEquals(ReminderScope.ROUND, reminder.scope());
    }

    @Test
    void rejectsBlankOrNestedReminderContent() {
        assertThrows(IllegalArgumentException.class,
                () -> new SystemReminder(ReminderScope.SESSION, " "));
        assertThrows(IllegalArgumentException.class,
                () -> new SystemReminder(ReminderScope.SESSION,
                        "<system-reminder>嵌套</system-reminder>"));
    }
}
