package io.imiocode.config;

import io.imiocode.hook.HookEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HookConfigLoaderIntegrationTest {
    @TempDir Path workspace;

    @Test void rootHooksLoadInDeclarationOrderAndNoHooksRemainsCompatible() throws Exception {
        write(base() + """
                hooks:
                  - id: first
                    event: startup
                    action:
                      type: prompt
                      message: hello
                  - id: second
                    event: shutdown
                    action:
                      type: command
                      command: echo done
                """);
        RuntimeConfig loaded = new ConfigLoader().loadAll(workspace, workspace, Map.of());
        assertEquals(java.util.List.of("first", "second"),
                loaded.hooks().hooks().stream().map(io.imiocode.hook.Hook::id).toList());
        assertEquals(HookEvent.STARTUP, loaded.hooks().hooks().getFirst().event());

        write(base());
        assertTrue(new ConfigLoader().loadAll(workspace, workspace, Map.of()).hooks().hooks().isEmpty());
    }

    @Test void anyInvalidHookDisablesEntireSetAndAggregatesDocuments() throws Exception {
        write(base() + """
                hooks:
                  - id: good
                    event: startup
                    action:
                      type: prompt
                      message: hello
                  - id: bad-event
                    event: unknown_event
                    action:
                      type: prompt
                      message: no
                  - id: bad-async
                    event: pre_tool_use
                    async: true
                    action:
                      type: command
                      command: echo no
                """);
        RuntimeConfig loaded = new ConfigLoader().loadAll(workspace, workspace, Map.of());
        assertTrue(loaded.hooks().hooks().isEmpty());
        assertEquals(2, loaded.hooks().errors().size());
    }

    @Test void checkedInExampleConfigurationIsLoadable() throws Exception {
        Files.copy(Path.of("config.example.yaml"), workspace.resolve("config.yaml"));
        RuntimeConfig loaded = new ConfigLoader().loadAll(workspace, workspace, Map.of(
                "OPENAI_API_KEY", "openai-test",
                "ANTHROPIC_API_KEY", "anthropic-test",
                "DEEPSEEK_API_KEY", "deepseek-test"));
        assertTrue(loaded.hooks().errors().isEmpty());
        assertTrue(loaded.hooks().hooks().isEmpty());
    }

    private String base() {
        return """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: test-key
                """;
    }

    private void write(String yaml) throws Exception {
        Files.writeString(workspace.resolve("config.yaml"), yaml, StandardCharsets.UTF_8);
    }
}
