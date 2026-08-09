package io.imiocode.permission.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShellCommandTokenizerTest {
    private final ShellCommandTokenizer tokenizer = new ShellCommandTokenizer();

    @Test
    void preservesQuotedAndEscapedArguments() {
        assertEquals(List.of("git", "commit", "-m", "hello world"),
                tokenizer.tokenize("git commit -m \"hello world\"").tokens());
        assertEquals(List.of("echo", "hello world"),
                tokenizer.tokenize("echo hello\\ world").tokens());
        assertEquals(List.of("echo", "a;b"), tokenizer.tokenize("echo 'a;b'").tokens());
    }

    @Test
    void rejectsBlankAndUnclosedQuotes() {
        assertFalse(tokenizer.tokenize(" ").valid());
        assertFalse(tokenizer.tokenize("echo \"secret").valid());
        assertTrue(tokenizer.tokenize("echo ok").valid());
    }
}
