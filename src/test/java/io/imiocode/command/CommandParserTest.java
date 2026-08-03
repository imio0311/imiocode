package io.imiocode.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandParserTest {
    private final CommandParser parser = new CommandParser();

    @Test
    void parsesWhitespaceQuotesAndEscapes() {
        ParsedCommand command = parser.parse("  /memory add user \"回答 保持简洁\"  ").orElseThrow();
        assertEquals("memory", command.name());
        assertEquals(java.util.List.of("add", "user", "回答 保持简洁"), command.arguments());
        assertTrue(parser.parse("普通消息").isEmpty());
    }

    @Test
    void preservesOrdinaryWindowsPathBackslashes() {
        ParsedCommand command = parser.parse("/memory add project C:\\work\\imiocode").orElseThrow();

        assertEquals(java.util.List.of("add", "project", "C:\\work\\imiocode"), command.arguments());
    }

    @Test
    void rejectsUnclosedQuote() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("/memory add user \"未完成"));
    }
}
