package io.imiocode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Ch9ConfigTest {
    @TempDir Path directory;

    @Test
    void loadsDefaultsAndOverrides() throws Exception {
        Files.writeString(directory.resolve("config.yaml"), """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: test-key
                instructions:
                  max-include-depth: 6
                sessions:
                  retention-days: 0
                  max-sessions: 12
                memory:
                  auto-extract: false
                  max-entry-chars: 800
                """);
        AppConfig config = new ConfigLoader().load(directory, Map.of());
        assertEquals(6, config.instructions().maxIncludeDepth());
        assertEquals(12, config.sessions().maxSessions());
        assertFalse(config.memory().autoExtract());
        assertEquals(800, config.memory().maxEntryChars());
    }

    @Test
    void rejectsHardLimit() throws Exception {
        Files.writeString(directory.resolve("config.yaml"), """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: test-key
                instructions:
                  max-include-depth: 33
                """);
        assertThrows(ConfigException.class, () -> new ConfigLoader().load(directory, Map.of()));
    }
}
