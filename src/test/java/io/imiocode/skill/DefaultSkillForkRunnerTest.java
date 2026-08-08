package io.imiocode.skill;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessageRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSkillForkRunnerTest {
    @Test
    void selectsFullRecentAndNoneHistory() {
        List<ChatMessage> history = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            history.add(new ChatMessage(index % 2 == 0 ? MessageRole.USER : MessageRole.ASSISTANT,
                    "message-" + index));
        }

        assertEquals(20, DefaultSkillForkRunner.selectHistory(history, SkillHistoryMode.FULL).size());
        assertTrue(DefaultSkillForkRunner.selectHistory(history, SkillHistoryMode.RECENT).size() <= 12);
        assertEquals(MessageRole.USER,
                DefaultSkillForkRunner.selectHistory(history, SkillHistoryMode.RECENT).getFirst().role());
        assertTrue(DefaultSkillForkRunner.selectHistory(history, SkillHistoryMode.NONE).isEmpty());
    }
}
