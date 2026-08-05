package io.imiocode.command.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewPromptBuilderTest {
    private final ReviewPromptBuilder builder = new ReviewPromptBuilder();

    @Test
    void basePromptIsStableAndReadOnly() {
        String first = builder.build("");
        assertEquals(first, builder.build(null));
        assertTrue(first.contains("只做审查，不要修改文件"));
        assertTrue(first.contains("findings"));
        assertFalse(first.contains("Additional focus"));
    }

    @Test
    void appendsFocusWithoutSlashSyntax() {
        String prompt = builder.build("重点关注并发安全");
        assertTrue(prompt.contains("Additional focus:\n重点关注并发安全"));
        assertFalse(prompt.contains("/review"));
    }
}
