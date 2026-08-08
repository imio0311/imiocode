package io.imiocode.skill.install;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteSkillLocatorTest {
    private final RemoteSkillLocator locator = new RemoteSkillLocator(SkillInstallConfig.defaults());

    @Test
    void parsesSupportedLocations() {
        RemoteSkillLocation skills = locator.locate(
                "https://skills.sh/anthropics/skills/frontend-design");
        assertEquals(RemoteSkillKind.SKILLS_SH, skills.kind());
        assertEquals("anthropics", skills.owner());
        assertEquals("skills/frontend-design", skills.path());

        RemoteSkillLocation tree = locator.locate(
                "https://github.com/acme/repo/tree/v1/agent-skills/design");
        assertEquals(RemoteSkillKind.GITHUB_TREE, tree.kind());
        assertEquals("v1", tree.revision());
        assertEquals("agent-skills/design", tree.path());

        RemoteSkillLocation raw = locator.locate(
                "https://raw.githubusercontent.com/acme/repo/main/skills/design/SKILL.md");
        assertEquals(RemoteSkillKind.GITHUB_RAW, raw.kind());
        assertEquals("skills/design/SKILL.md", raw.path());
    }

    @Test
    void rejectsUnsafeAndUnsupportedLocations() {
        assertThrows(SkillInstallException.class,
                () -> locator.locate("http://skills.sh/a/b/c"));
        assertThrows(SkillInstallException.class,
                () -> locator.locate("https://example.com/a/b/c"));
        assertThrows(SkillInstallException.class,
                () -> locator.locate("https://github.com/a/b/tree/main/%2e%2e/secret"));
        assertThrows(SkillInstallException.class,
                () -> locator.locate("https://raw.githubusercontent.com/a/b/main/x.txt"));
        assertThrows(SkillInstallException.class,
                () -> locator.locate("https://skills.sh/a/b/c?x=1"));
        assertThrows(SkillInstallException.class,
                () -> locator.locate("https://user@skills.sh/a/b/c"));
        assertThrows(SkillInstallException.class,
                () -> locator.locate("https://skills.sh:443/a/b/c"));
        assertThrows(SkillInstallException.class,
                () -> locator.locate("https://skills.sh/a/b/%2Fsecret"));
        assertThrows(SkillInstallException.class,
                () -> locator.locate("https://skills.sh/a/b/%5Csecret"));
    }
}
