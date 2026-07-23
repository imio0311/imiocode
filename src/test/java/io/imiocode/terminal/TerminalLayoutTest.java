package io.imiocode.terminal;

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
        for (int width : List.of(20, 40, 60, 100)) {
            TerminalMode mode = TerminalMode.select(width, true);
            List<String> lines = layout.welcome(context, UiState.READY, width, mode);
            lines.forEach(line -> assertTrue(
                    TerminalLayout.columns(line) <= width,
                    () -> "宽度 " + width + " 越界: " + line));
            assertTrue(TerminalLayout.columns(layout.inputTop(width, mode)) <= width);
            assertTrue(TerminalLayout.columns(layout.statusLine(context, UiState.STREAMING, width, mode)) <= width);
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
    }
}
