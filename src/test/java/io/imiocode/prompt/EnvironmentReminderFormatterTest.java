package io.imiocode.prompt;

import io.imiocode.conversation.ReminderScope;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentReminderFormatterTest {

    @Test
    void formatsStableEnvironmentFieldsWithoutDirtyFileDetails() {
        var context = new EnvironmentContext(
                Path.of("D:/workspace").toAbsolutePath(),
                "Windows 11",
                "amd64",
                "PowerShell",
                ZonedDateTime.of(2026, 7, 29, 10, 30, 0, 0,
                        ZoneId.of("Asia/Shanghai")),
                new GitContext(Optional.of("feature/ch5"), GitWorkingTreeState.DIRTY),
                "model-sentinel"
        );

        var reminder = new EnvironmentReminderFormatter().format(context);
        String content = reminder.content();

        assertEquals(ReminderScope.ENVIRONMENT, reminder.scope());
        assertTrue(content.contains("工作目录："));
        assertTrue(content.contains("操作系统：Windows 11"));
        assertTrue(content.contains("系统架构：amd64"));
        assertTrue(content.contains("Shell：PowerShell"));
        assertTrue(content.contains("当前时间：2026-07-29"));
        assertTrue(content.contains("Git 仓库：是"));
        assertTrue(content.contains("Git 分支：feature/ch5"));
        assertTrue(content.contains("Git 状态：dirty"));
        assertTrue(content.contains("当前模型：model-sentinel"));
        assertFalse(content.contains("secret-file-name.txt"));
        assertTrue(reminder.wrappedContent().startsWith("<system-reminder>"));
        assertFalse(SystemPromptBuilder.defaults().build().contains("model-sentinel"));
    }

    @Test
    void rendersAllGitAvailabilityStates() {
        var formatter = new EnvironmentReminderFormatter();
        assertTrue(formatter.format(contextWith(GitWorkingTreeState.CLEAN))
                .content().contains("Git 状态：clean"));
        assertTrue(formatter.format(contextWith(GitWorkingTreeState.DIRTY))
                .content().contains("Git 状态：dirty"));
        assertTrue(formatter.format(contextWith(GitWorkingTreeState.NOT_REPOSITORY))
                .content().contains("Git 状态：not-repository"));
        assertTrue(formatter.format(contextWith(GitWorkingTreeState.UNAVAILABLE))
                .content().contains("Git 状态：unavailable"));
        assertTrue(formatter.format(contextWith(GitWorkingTreeState.CLEAN))
                .content().contains("Git 仓库：是"));
        assertTrue(formatter.format(contextWith(GitWorkingTreeState.NOT_REPOSITORY))
                .content().contains("Git 仓库：否"));
        assertTrue(formatter.format(contextWith(GitWorkingTreeState.UNAVAILABLE))
                .content().contains("Git 仓库：未知"));
    }

    @Test
    void legacyEnvironmentContextUsesUnknownDynamicFields() {
        EnvironmentContext context = contextWith(GitWorkingTreeState.CLEAN);
        String content = new EnvironmentReminderFormatter().format(context).content();

        assertEquals("unknown", context.architecture());
        assertEquals("unknown", context.model());
        assertTrue(content.contains("系统架构：unknown"));
        assertTrue(content.contains("当前模型：unknown"));
    }

    private static EnvironmentContext contextWith(GitWorkingTreeState state) {
        return new EnvironmentContext(
                Path.of("D:/workspace").toAbsolutePath(),
                "Windows",
                "PowerShell",
                ZonedDateTime.of(2026, 7, 29, 10, 30, 0, 0,
                        ZoneId.of("Asia/Shanghai")),
                new GitContext(Optional.empty(), state));
    }
}
