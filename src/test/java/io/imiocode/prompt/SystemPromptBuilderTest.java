package io.imiocode.prompt;

import io.imiocode.prompt.section.BehaviorSection;
import io.imiocode.prompt.section.CodeQualitySection;
import io.imiocode.prompt.section.IdentitySection;
import io.imiocode.prompt.section.OutputStyleSection;
import io.imiocode.prompt.section.SecuritySection;
import io.imiocode.prompt.section.TaskPatternSection;
import io.imiocode.prompt.section.ToolUsageSection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemPromptBuilderTest {

    @Test
    void buildsSevenSectionsInStablePriorityOrder() {
        var builder = new SystemPromptBuilder(List.of(
                new OutputStyleSection(),
                new SecuritySection(),
                new IdentitySection(),
                new TaskPatternSection(),
                new ToolUsageSection(),
                new CodeQualitySection(),
                new BehaviorSection()
        ));

        String first = builder.build();
        String second = builder.build();

        assertEquals(first, second);
        assertBefore(first, "## 身份", "## 行为");
        assertBefore(first, "## 行为", "## 工具使用");
        assertBefore(first, "## 工具使用", "## 代码质量");
        assertBefore(first, "## 代码质量", "## 安全");
        assertBefore(first, "## 安全", "## 任务模式");
        assertBefore(first, "## 任务模式", "## 输出风格");
    }

    @Test
    void skipsBlankSectionsAndUsesNameAsStableTieBreaker() {
        PromptSection beta = () -> new Section("乙", SectionPriority.BEHAVIOR, "内容乙");
        PromptSection alpha = () -> new Section("甲", SectionPriority.BEHAVIOR, "内容甲");
        PromptSection blank = () -> new Section("空白", SectionPriority.IDENTITY, " ");

        String prompt = new SystemPromptBuilder(List.of(beta, blank, alpha)).build();

        String expectedFirst = "甲".compareTo("乙") < 0 ? "## 甲" : "## 乙";
        String expectedSecond = expectedFirst.equals("## 甲") ? "## 乙" : "## 甲";
        assertTrue(prompt.indexOf(expectedFirst) < prompt.indexOf(expectedSecond));
        assertTrue(!prompt.contains("## 空白"));
    }

    @Test
    void rejectsDuplicateSectionNames() {
        PromptSection first = () -> new Section("重复", SectionPriority.IDENTITY, "一");
        PromptSection second = () -> new Section("重复", SectionPriority.BEHAVIOR, "二");

        assertThrows(IllegalArgumentException.class,
                () -> new SystemPromptBuilder(List.of(first, second)).build());
    }

    private static void assertBefore(String text, String first, String second) {
        assertTrue(text.indexOf(first) >= 0, () -> "缺少标题：" + first);
        assertTrue(text.indexOf(second) >= 0, () -> "缺少标题：" + second);
        assertTrue(text.indexOf(first) < text.indexOf(second),
                () -> first + " 应位于 " + second + " 之前");
    }
}
