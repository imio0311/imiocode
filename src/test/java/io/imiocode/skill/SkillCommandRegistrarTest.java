package io.imiocode.skill;

import io.imiocode.command.CommandRegistry;
import io.imiocode.command.builtin.HelpCommand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillCommandRegistrarTest {
    @Test
    void atomicallyReplacesDynamicCommandsAndCompletion() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new HelpCommand());
        SkillCommandRegistrar registrar = new SkillCommandRegistrar(registry);

        registrar.sync(snapshot(1, "commit"));
        assertTrue(registry.find("commit").isPresent());
        assertTrue(registry.complete("co").contains("/commit"));

        registrar.sync(snapshot(2, "test"));
        assertTrue(registry.find("commit").isEmpty());
        assertTrue(registry.find("test").isPresent());
        assertTrue(registry.find("help").isPresent());
        assertEquals(2, registry.listCommands().size());
    }

    private static SkillCatalogSnapshot snapshot(long generation, String name) {
        SkillMetadata metadata = new SkillMetadata(name, "描述", SkillMode.INLINE,
                SkillHistoryMode.RECENT, Set.of(), Set.of());
        SkillSource source = new SkillSource() {
            @Override public String id() { return "memory:" + name; }
            @Override public String readFrontmatter() { return ""; }
            @Override public String readMarkdown() { return ""; }
            @Override public Optional<String> readToolJson() { return Optional.empty(); }
            @Override public List<SkillReference> readReferences() { return List.of(); }
        };
        return new SkillCatalogSnapshot(generation,
                Map.of(name, new SkillDescriptor(metadata, SkillOrigin.PROJECT, source)), List.of());
    }
}
