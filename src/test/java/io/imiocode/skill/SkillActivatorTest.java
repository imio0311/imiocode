package io.imiocode.skill;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.ToolSelection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillActivatorTest {
    @Test
    void combinesActiveWhitelistsAndKeepsSystemTool() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(tool("read_file"));
        registry.register(tool("bash"));
        registry.register(tool("load_skill"));
        SkillActivator activator = new SkillActivator(registry, ignored -> tool("unused"));

        try (SkillRunScope ignored = activator.beginRun(Optional.empty(), List.of())) {
            activator.activate(loaded("one", Set.of("read_file")));
            activator.activate(loaded("two", Set.of("bash")));
            activator.activate(loaded("one", Set.of("read_file")));

            ToolSelection normal = activator.selectTools(ToolSelection.allEnabled());
            assertTrue(normal.allows("read_file"));
            assertTrue(normal.allows("bash"));
            assertTrue(normal.allows("load_skill"));
            assertFalse(normal.allows("write_file"));
            assertEquals(List.of("one", "two"), activator.activeSkillNames());

            ToolSelection plan = activator.selectTools(ToolSelection.only(Set.of("read_file")));
            assertTrue(plan.allows("read_file"));
            assertTrue(plan.allows("load_skill"));
            assertFalse(plan.allows("bash"));
            assertTrue(activator.activeReminder().orElseThrow().content().contains("<skill name=\"one\">"));
        }
        assertTrue(activator.activeSkillNames().isEmpty());
    }

    @Test
    void missingAllowedToolFailsBeforeRun() {
        SkillActivator activator = new SkillActivator(new ToolRegistry(), ignored -> tool("unused"));
        SkillException error = assertThrows(SkillException.class,
                () -> activator.validate(loaded("broken", Set.of("missing"))));
        assertTrue(error.getMessage().contains("missing"));
    }

    @Test
    void dedicatedToolExistsOnlyInsideActivationScope() {
        ToolRegistry registry = new ToolRegistry();
        SkillActivator activator = new SkillActivator(registry,
                spec -> tool(spec.exposedName()));
        var schema = JsonNodeFactory.instance.objectNode().put("type", "object");
        SkillToolSpec spec = new SkillToolSpec("format", "skill_pack_format", "格式化",
                schema, "echo", List.of("ok"), ToolRisk.LOW);
        SkillMetadata metadata = new SkillMetadata("pack", "目录包", SkillMode.INLINE,
                SkillHistoryMode.RECENT, Set.of(), Set.of("format"));
        LoadedSkill loaded = new LoadedSkill(
                new SkillDescriptor(metadata, SkillOrigin.PROJECT, memorySource("pack")),
                "SOP", List.of(new SkillReference("guide.md", "参考内容")),
                List.of(spec), Set.of("skill_pack_format"));

        assertFalse(registry.registeredNames().contains("skill_pack_format"));
        try (SkillRunScope ignored = activator.beginRun(Optional.empty(), List.of())) {
            activator.activate(loaded);
            ToolSelection selection = activator.selectTools(ToolSelection.allEnabled());
            assertTrue(registry.enabledDefinitions(selection).stream()
                    .anyMatch(value -> value.name().equals("skill_pack_format")));
            assertTrue(activator.activeReminder().orElseThrow().content().contains("参考内容"));
        }
        assertFalse(registry.registeredNames().contains("skill_pack_format"));
    }

    private static LoadedSkill loaded(String name, Set<String> allowed) {
        SkillMetadata metadata = new SkillMetadata(name, "描述", SkillMode.INLINE,
                SkillHistoryMode.RECENT, Set.of(), allowed);
        SkillSource source = memorySource(name);
        return new LoadedSkill(new SkillDescriptor(metadata, SkillOrigin.PROJECT, source),
                "SOP " + name, List.of(), List.of(), allowed);
    }

    private static SkillSource memorySource(String name) {
        return new SkillSource() {
            @Override public String id() { return "memory:" + name; }
            @Override public String readFrontmatter() { return ""; }
            @Override public String readMarkdown() { return ""; }
            @Override public Optional<String> readToolJson() { return Optional.empty(); }
            @Override public List<SkillReference> readReferences() { return List.of(); }
        };
    }

    private static Tool tool(String name) {
        return new Tool() {
            private final ToolDefinition definition = new ToolDefinition(name, "测试工具",
                    JsonNodeFactory.instance.objectNode().put("type", "object"), ToolRisk.LOW);
            @Override public ToolDefinition definition() { return definition; }
            @Override public ToolResult execute(com.fasterxml.jackson.databind.node.ObjectNode arguments) {
                return ToolResult.success("ok");
            }
        };
    }
}
