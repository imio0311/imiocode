package io.imiocode.terminal;

import io.imiocode.permission.PermissionPrompt;
import io.imiocode.permission.PermissionReply;
import io.imiocode.tool.ToolRisk;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JLinePermissionPromptTest {
    @Test
    void retriesInvalidInputAndReturnsSessionChoice() throws Exception {
        ByteArrayInputStream input =
                new ByteArrayInputStream("invalid\n2\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .type(Terminal.TYPE_DUMB)
                .streams(input, output)
                .encoding(StandardCharsets.UTF_8)
                .build();
        JLineTerminalUi ui = new JLineTerminalUi(terminal);

        PermissionReply reply = ui.confirmPermission(new PermissionPrompt(
                "permission-1", 1, "bash", ToolRisk.HIGH, "mvn test", "需要确认"));

        assertEquals(PermissionReply.ALLOW_SESSION, reply);
        String rendered = output.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("权限确认"));
        assertTrue(rendered.contains("mvn test"));
        assertTrue(rendered.contains("请输入 1、2 或 3"));
        ui.close();
    }
}
