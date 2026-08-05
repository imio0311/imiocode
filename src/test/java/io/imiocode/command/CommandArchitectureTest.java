package io.imiocode.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandArchitectureTest {
    @Test
    void oldContractsAreRemovedAndCommandPackageDoesNotDependOnJline() throws IOException {
        Path commandRoot = Path.of("src/main/java/io/imiocode/command");
        assertFalse(Files.exists(commandRoot.resolve("LocalCommand.java")));
        assertFalse(Files.exists(commandRoot.resolve("LocalCommandRegistry.java")));
        assertFalse(Files.exists(commandRoot.resolve("CommandDisposition.java")));
        assertFalse(Files.exists(Path.of("src/main/java/io/imiocode/terminal/ConfirmationPrompt.java")));

        try (var files = Files.walk(commandRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                assertFalse(source.contains("org.jline"), file.toString());
                assertFalse(source.contains("JLineTerminalUi"), file.toString());
                assertFalse(source.contains("io.imiocode.runtime.ConversationLoop"), file.toString());
            }
        }
        assertTrue(Files.exists(commandRoot.resolve("CommandRegistry.java")));
    }
}
