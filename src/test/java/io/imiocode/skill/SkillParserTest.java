package io.imiocode.skill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillParserTest {
    private final SkillParser parser = new SkillParser();

    @Test
    void descriptorOnlyReadsFrontmatterAndFullLoadReplacesArguments() {
        CountingSource source = new CountingSource("""
                ---
                name: demo
                description: 演示技能
                mode: fork
                history: none
                allowedTools: [read_file]
                ---
                请处理：$ARGUMENTS
                """);

        SkillDescriptor descriptor = parser.parseDescriptor(source, SkillOrigin.PROJECT);

        assertEquals(1, source.frontmatterReads.get());
        assertEquals(0, source.markdownReads.get());
        assertEquals(SkillMode.FORK, descriptor.metadata().mode());
        assertEquals(SkillHistoryMode.NONE, descriptor.metadata().history());

        LoadedSkill loaded = parser.parseLoaded(descriptor, "$1\\path");
        assertEquals("请处理：$1\\path", loaded.prompt());
        assertEquals(1, source.markdownReads.get());
    }

    @Test
    void rejectsMissingBodyAndInvalidName() {
        CountingSource empty = new CountingSource("""
                ---
                name: demo
                description: 演示
                ---
                """);
        SkillDescriptor descriptor = parser.parseDescriptor(empty, SkillOrigin.USER);
        assertThrows(SkillException.class, () -> parser.parseLoaded(descriptor, ""));

        CountingSource invalid = new CountingSource("""
                ---
                name: Bad Name
                description: 演示
                ---
                body
                """);
        assertThrows(SkillException.class,
                () -> parser.parseDescriptor(invalid, SkillOrigin.USER));
    }

    @Test
    void parsesDirectoryToolsOnlyDuringFullLoad() {
        CountingSource source = new CountingSource("""
                ---
                name: package
                description: 目录技能
                allowedTools: [formatter]
                ---
                格式化 $ARGUMENTS
                """);
        source.toolJson = Optional.of("""
                {"name":"formatter","description":"格式化", "command":"fmt",
                 "args":["${path}"], "inputSchema":{"type":"object","properties":{"path":{"type":"string"}}}}
                """);

        SkillDescriptor descriptor = parser.parseDescriptor(source, SkillOrigin.PROJECT);
        assertEquals(0, source.toolReads.get());
        LoadedSkill loaded = parser.parseLoaded(descriptor, "src");

        assertEquals(1, source.toolReads.get());
        assertEquals("skill_package_formatter", loaded.tools().getFirst().exposedName());
        assertTrue(loaded.allowedTools().contains("skill_package_formatter"));
    }

    @Test
    void standardSkillWithoutWhitelistGetsSafeFileDefaultsButExplicitEmptyStaysEmpty() {
        CountingSource standard = new CountingSource("""
                ---
                name: standard
                description: 标准 Agent Skill
                ---
                设计界面
                """);
        LoadedSkill loaded = parser.parseLoaded(
                parser.parseDescriptor(standard, SkillOrigin.PROJECT), "");
        assertEquals(java.util.Set.of("read_file", "write_file", "edit_file", "glob", "grep"),
                loaded.allowedTools());

        CountingSource locked = new CountingSource("""
                ---
                name: locked
                description: 不开放工具
                allowedTools: []
                ---
                只输出建议
                """);
        LoadedSkill empty = parser.parseLoaded(
                parser.parseDescriptor(locked, SkillOrigin.PROJECT), "");
        assertTrue(empty.allowedTools().isEmpty());
    }

    private static final class CountingSource implements SkillSource {
        private final String markdown;
        private final AtomicInteger frontmatterReads = new AtomicInteger();
        private final AtomicInteger markdownReads = new AtomicInteger();
        private final AtomicInteger toolReads = new AtomicInteger();
        private Optional<String> toolJson = Optional.empty();

        private CountingSource(String markdown) { this.markdown = markdown; }
        @Override public String id() { return "memory:skill"; }
        @Override public String readFrontmatter() {
            frontmatterReads.incrementAndGet();
            String normalized = markdown.replace("\r\n", "\n");
            int end = normalized.indexOf("\n---\n", 4);
            if (!normalized.startsWith("---\n") || end < 0) {
                throw new SkillException("frontmatter 无效");
            }
            return normalized.substring(4, end);
        }
        @Override public String readMarkdown() { markdownReads.incrementAndGet(); return markdown; }
        @Override public Optional<String> readToolJson() { toolReads.incrementAndGet(); return toolJson; }
        @Override public List<SkillReference> readReferences() throws IOException { return List.of(); }
    }
}
