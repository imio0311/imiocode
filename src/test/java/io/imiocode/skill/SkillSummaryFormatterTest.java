package io.imiocode.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillSummaryFormatterTest {
    @Test
    void summaryContainsOnlyNameAndDescription() {
        SkillMetadata metadata = new SkillMetadata("demo", "公开描述", SkillMode.INLINE,
                SkillHistoryMode.RECENT, Set.of(), Set.of("read_file"));
        SkillSource source = new SkillSource() {
            @Override public String id() { return "memory:demo"; }
            @Override public String readFrontmatter() { return ""; }
            @Override public String readMarkdown() { return "绝密 SOP"; }
            @Override public Optional<String> readToolJson() { return Optional.of("专属工具内容"); }
            @Override public List<SkillReference> readReferences() {
                return List.of(new SkillReference("secret.md", "参考资料内容"));
            }
        };
        SkillCatalogSnapshot snapshot = new SkillCatalogSnapshot(1,
                Map.of("demo", new SkillDescriptor(metadata, SkillOrigin.PROJECT, source)), List.of());

        String content = new SkillSummaryFormatter().format(snapshot).content();

        assertTrue(content.contains("demo: 公开描述"));
        assertFalse(content.contains("绝密 SOP"));
        assertFalse(content.contains("专属工具内容"));
        assertFalse(content.contains("参考资料内容"));
    }
}
