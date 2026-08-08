package io.imiocode.skill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillDirectoryPackageTest {
    @TempDir Path temp;

    @Test
    void loadsSkillMarkdownToolJsonAndReferencesAsOnePackage() throws Exception {
        Path pack = Files.createDirectories(temp.resolve("formatter"));
        Files.writeString(pack.resolve("SKILL.md"), """
                ---
                name: formatter
                description: 目录能力包
                allowedTools: [format]
                ---
                按参考规范处理 $ARGUMENTS
                """);
        Files.writeString(pack.resolve("tool.json"), """
                {"name":"format","description":"执行格式化", "command":"fmt",
                 "args":["${path}"], "inputSchema":{"type":"object","properties":{"path":{"type":"string"}}}}
                """);
        Path references = Files.createDirectories(pack.resolve("references"));
        Files.writeString(references.resolve("style.md"), "只使用四个空格");

        SkillParser parser = new SkillParser();
        SkillDescriptor descriptor = parser.parseDescriptor(
                new FileSkillSource(pack), SkillOrigin.PROJECT);
        LoadedSkill loaded = parser.parseLoaded(descriptor, "src/Main.java");

        assertTrue(loaded.prompt().contains("src/Main.java"));
        assertEquals("style.md", loaded.references().getFirst().path());
        assertEquals("只使用四个空格", loaded.references().getFirst().content());
        assertEquals("skill_formatter_format", loaded.tools().getFirst().exposedName());
        assertTrue(loaded.allowedTools().contains("skill_formatter_format"));
    }

    @Test
    void rejectsRelativePathEscapingPackage() throws Exception {
        Path pack = Files.createDirectories(temp.resolve("safe-pack"));
        Path outside = temp.resolve("outside.txt");
        Files.writeString(outside, "outside");

        SkillException error = assertThrows(SkillException.class,
                () -> FileSkillSource.ensureLexicallyContained(
                        pack, pack.resolve("references").resolve("..").resolve("..").resolve("outside.txt")));
        assertTrue(error.getMessage().contains("越界"));
    }
}
