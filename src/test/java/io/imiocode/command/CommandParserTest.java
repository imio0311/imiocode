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

        ParsedCommand quoted = parser.parse("/memory add project \"C:\\work space\\project\"").orElseThrow();
        assertEquals("C:\\work space\\project", quoted.arguments().get(2));
        ParsedCommand trailing = parser.parse("/memory add project C:\\work\\project\\").orElseThrow();
        assertEquals("C:\\work\\project\\", trailing.arguments().get(2));
    }

    @Test
    void parsesCaseSingleQuotesEscapesAndChinese() {
        ParsedCommand command = parser.parse("  /MeMoRy add user '中文 内容' escaped\\ value  ").orElseThrow();
        assertEquals("memory", command.name());
        assertEquals(java.util.List.of("add", "user", "中文 内容", "escaped value"), command.arguments());
    }

    @Test
    void rejectsUnclosedQuote() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("/memory add user \"未完成"));
    }
}
