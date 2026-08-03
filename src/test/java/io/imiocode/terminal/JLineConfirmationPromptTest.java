package io.imiocode.terminal;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JLineConfirmationPromptTest {
    @Test
    void acceptsOnlyExplicitYes() throws Exception {
        assertTrue(confirm("y\n"));
        assertTrue(confirm("YES\n"));
        assertFalse(confirm("\n"));
        assertFalse(confirm("n\n"));
        assertFalse(confirm("unknown\n"));
        assertFalse(confirm(""));
    }

    private boolean confirm(String input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal);
        try {
            boolean result = ui.confirmAction(new ConfirmationPrompt(
                    "删除会话", "abc123", "删除后无法恢复"));
            String rendered = output.toString(StandardCharsets.UTF_8);
            assertTrue(rendered.contains("删除会话"), rendered);
            assertTrue(rendered.contains("[y/N]"), rendered);
            return result;
        } finally {
            ui.close();
        }
    }
}
