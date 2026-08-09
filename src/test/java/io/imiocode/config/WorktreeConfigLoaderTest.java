package io.imiocode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorktreeConfigLoaderTest {
    @TempDir Path temp;

    @Test void loadsDefaultsAndExample() throws Exception {
        Path workspace = temp.resolve("workspace"); Path user = temp.resolve("user");
        Files.createDirectories(workspace); Files.createDirectories(user);
        RuntimeConfig defaults = new ConfigLoader().loadAll(workspace, user,
                Map.of("IMIO_PROVIDER", "deepseek", "IMIO_MODEL", "deepseek-chat",
                        "DEEPSEEK_API_KEY", "key"));
        assertEquals(Path.of(".imiocode/worktrees"), defaults.worktrees().directory());
        Files.copy(Path.of("config.example.yaml"), workspace.resolve("config.yaml"));
        RuntimeConfig example = new ConfigLoader().loadAll(workspace, user, Map.of("DEEPSEEK_API_KEY", "key"));
        assertEquals(168, example.worktrees().staleAfter().toHours());
        assertTrue(example.worktrees().linkDirectories().contains("node_modules"));
    }

    @Test void rejectsDirectoryOutsideManagedNamespace() throws Exception {
        Path workspace = temp.resolve("workspace"); Path user = temp.resolve("user");
        Files.createDirectories(workspace); Files.createDirectories(user);
        Files.writeString(workspace.resolve("config.yaml"), """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: key
                worktrees:
                  directory: ../outside
                """);
        assertThrows(ConfigException.class, () -> new ConfigLoader().loadAll(workspace, user, Map.of()));
    }
}
