package io.imiocode.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SubagentConfigLoaderTest {
    @TempDir Path temp;

    @Test void realExampleYamlLoadsSubagentDefaultsAndAliasesSection() throws Exception {
        Path workspace=temp.resolve("workspace"); Path user=temp.resolve("user");
        Files.createDirectories(workspace); Files.createDirectories(user);
        Files.copy(Path.of("config.example.yaml"),workspace.resolve("config.yaml"));
        RuntimeConfig loaded=new ConfigLoader().loadAll(workspace,user,Map.of("DEEPSEEK_API_KEY","test-key"));
        assertEquals(4,loaded.subagents().maxBackgroundTasks());
        assertEquals(java.util.Set.of("read_file","glob","grep"),loaded.subagents().backgroundAllowedTools());
        assertTrue(loaded.subagents().globallyDeniedTools().contains("agent"));
    }
}
