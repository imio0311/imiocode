package io.imiocode.skill;

import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;

import java.util.Objects;

/** 只把名称和描述注入常驻消息，不泄露完整 Skill。 */
public final class SkillSummaryFormatter {
    public SystemReminder format(SkillCatalogSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        StringBuilder content = new StringBuilder("可用 Skill（需要时调用 load_skill 加载完整指令）：");
        if (snapshot.skills().isEmpty()) {
            content.append("\n- 当前没有可用 Skill");
        } else {
            snapshot.sorted().forEach(skill -> content.append("\n- ")
                    .append(skill.metadata().name()).append(": ")
                    .append(skill.metadata().description()));
        }
        return new SystemReminder(ReminderScope.SESSION, content.toString());
    }
}
