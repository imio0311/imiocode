package io.imiocode;

import io.imiocode.command.CommandDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ch10ApplicationTest {
    @Test
    void applicationRegistersTenCoreAndAllCompatibilityNames() {
        var registry = ImioCodeApplication.createCommandRegistry();
        Set<String> core = registry.listCommands().stream()
                .filter(item -> !item.compatibility())
                .map(CommandDescriptor::name)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("help", "compact", "clear", "plan", "do", "session",
                "memory", "permission", "status", "review"), core);
        for (String value : Set.of("exit", "quit", "verbose", "compact-ui")) {
            assertTrue(registry.find(value).isPresent(), value);
        }
        for (String alias : Set.of("h", "?", "cls", "sessions", "mem", "perm", "st", "rv", "quit")) {
            assertTrue(registry.complete(alias).contains("/" + alias), alias);
        }
    }
}
