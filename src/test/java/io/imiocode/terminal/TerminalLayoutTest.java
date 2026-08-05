package io.imiocode.terminal;

import io.imiocode.config.UiVerbosity;
import io.imiocode.agent.AgentMode;
import io.imiocode.command.CommandStatus;
import io.imiocode.permission.PermissionMode;
import io.imiocode.session.SessionId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalLayoutTest {
    private final TerminalLayout layout = new TerminalLayout();
    private final UiContext context = new UiContext(
            "ImioCode",
            "0.2.0-SNAPSHOT",
            "deepseek",
            "一个很长的模型名称-deepseek-chat-with-extra-suffix",
            Path.of("一个包含中文且非常长的工作目录", "project"));

    @Test
    void selectsModeFromWidthAndAnsiCapability() {
        assertEquals(TerminalMode.PLAIN, TerminalMode.select(20, true));
        assertEquals(TerminalMode.COMPACT, TerminalMode.select(40, true));
        assertEquals(TerminalMode.FULL, TerminalMode.select(60, true));
        assertEquals(TerminalMode.FULL, TerminalMode.select(100, true));
        assertEquals(TerminalMode.PLAIN, TerminalMode.select(100, false));
    }

    @Test
    void everyRenderedLineFitsTerminalWidth() {
        for (int width : List.of(20, 40, 60, 80, 100, 200)) {
            TerminalMode mode = TerminalMode.select(width, true);
            List<String> lines = layout.welcome(context, UiState.READY, width, mode);
            lines.forEach(line -> assertTrue(
                    TerminalLayout.columns(line) <= width,
                    () -> "宽度 " + width + " 越界: " + line));
            assertTrue(TerminalLayout.columns(layout.inputTop(width, mode)) <= width);
            assertTrue(TerminalLayout.columns(layout.statusLine(context, UiState.STREAMING, width, mode)) <= width);
            assertTrue(TerminalLayout.columns(layout.statusLine(
                    context, UiState.TOOL_WAITING, width, mode)) <= width);
            assertTrue(TerminalLayout.columns(layout.statusLine(
                    context, UiState.TOOL_RUNNING, width, mode)) <= width);
        }
    }

    @Test
    void fullAndPlainModesExposeRequiredContext() {
        UiContext shortContext = new UiContext(
                "ImioCode", "dev", "deepseek", "deepseek-chat", Path.of("work"));

        String full = String.join("\n", layout.welcome(shortContext, UiState.READY, 100, TerminalMode.FULL));
        assertTrue(full.contains("ImioCode vdev"));
        assertTrue(full.contains("deepseek"));
        assertTrue(full.contains("deepseek-chat"));
        assertTrue(full.contains(shortContext.workingDirectory().toString()));
        assertTrue(full.contains("Ready"));

        String plain = String.join("\n", layout.welcome(shortContext, UiState.ERROR, 100, TerminalMode.PLAIN));
        assertTrue(plain.contains("ImioCode vdev"));
        assertTrue(plain.contains("deepseek | deepseek-chat"));
        assertTrue(plain.contains(shortContext.workingDirectory().toString()));
        assertTrue(plain.contains("Error"));
        assertTrue(layout.statusLine(shortContext, UiState.TOOL_WAITING, 100, TerminalMode.PLAIN)
                .contains("Tool waiting"));
    }

    @Test
    void welcomeRestoresOriginalPanelWhileCompactConversationRemainsShort() {
        UiContext shortContext = new UiContext(
                "ImioCode", "dev", "deepseek", "deepseek-chat", Path.of("work"));

        String welcome = String.join("\n", layout.welcome(
                shortContext, UiState.READY, 100, TerminalMode.FULL));
        assertTrue(welcome.contains("___"));
        assertTrue(welcome.contains("┌"));
        assertTrue(welcome.contains("ImioCode vdev"));
        assertTrue(welcome.contains("Provider  deepseek"));
        assertTrue(welcome.contains("Model     deepseek-chat"));
        assertTrue(welcome.contains("Directory"));
        assertTrue(welcome.contains(shortContext.workingDirectory().toString()));
        assertTrue(welcome.contains("Status    Ready"));

        for (TerminalMode mode : TerminalMode.values()) {
            assertEquals("", layout.inputTop(100, mode, UiVerbosity.COMPACT));
            assertEquals("", layout.statusLine(
                    shortContext, UiState.THINKING, 100, mode, UiVerbosity.COMPACT));
        }
        assertEquals("> ", layout.primaryPrompt(TerminalMode.PLAIN, UiVerbosity.COMPACT));
        assertEquals("› ", layout.primaryPrompt(TerminalMode.FULL, UiVerbosity.COMPACT));
    }

    @Test
    void compactAndVerboseLayoutsFitEverySupportedWidth() {
        for (int width : List.of(20, 40, 60, 80, 100, 200)) {
            TerminalMode mode = TerminalMode.select(width, true);
            layout.welcome(context, UiState.READY, width, mode)
                    .forEach(line -> assertTrue(
                            TerminalLayout.columns(line) <= width,
                            () -> "启动面板宽度 " + width + " 越界: " + line));
            for (UiVerbosity verbosity : UiVerbosity.values()) {
                assertTrue(TerminalLayout.columns(
                        layout.inputTop(width, mode, verbosity)) <= width);
                assertTrue(TerminalLayout.columns(layout.statusLine(
                        context, UiState.STREAMING, width, mode, verbosity)) <= width);
            }
        }
    }

    @Test
    void narrowAndPlainWelcomeKeepOriginalResponsiveFallbacks() {
        UiContext shortContext = new UiContext(
                "ImioCode", "dev", "deepseek", "deepseek-chat", Path.of("work"));

        String narrow = String.join("\n", layout.welcome(
                shortContext, UiState.READY, 40, TerminalMode.COMPACT));
        assertTrue(narrow.contains("┌"));
        assertTrue(narrow.contains("Provider"));
        assertTrue(narrow.contains("Status"));
        assertTrue(!narrow.contains("|_ _|"));

        String plain = String.join("\n", layout.welcome(
                shortContext, UiState.READY, 100, TerminalMode.PLAIN));
        assertTrue(plain.contains("ImioCode vdev"));
        assertTrue(plain.contains("deepseek | deepseek-chat"));
        assertTrue(plain.contains("目录:"));
        assertTrue(plain.contains(shortContext.workingDirectory().toString()));
        assertTrue(plain.contains("状态: Ready"));
        assertTrue(!plain.contains("\u001B["));
    }

    @Test
    void dynamicStatusContainsModesAndFitsNarrowWidths() {
        CommandStatus status = new CommandStatus(
                "deepseek", "deepseek-chat", Path.of("work"), AgentMode.PLAN,
                PermissionMode.READ_ONLY, new SessionId("0123456789abcdef01234567"),
                8_200, 64_000, 1, 2);

        String verbose = layout.commandStatusLine(
                status, UiState.TOOL_RUNNING, 120, TerminalMode.FULL, UiVerbosity.VERBOSE);
        assertTrue(verbose.contains("Tool running"));
        assertTrue(verbose.contains("plan"));
        assertTrue(verbose.contains("read-only"));
        assertTrue(verbose.contains("MCP 1/2"));

        for (int width : List.of(10, 20, 40, 80, 120)) {
            for (UiVerbosity value : UiVerbosity.values()) {
                String line = layout.commandStatusLine(
                        status, UiState.READY, width, TerminalMode.FULL, value);
                assertTrue(TerminalLayout.columns(line) <= width, line);
            }
        }
    }
}
