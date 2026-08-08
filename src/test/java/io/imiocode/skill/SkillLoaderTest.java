package io.imiocode.skill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillLoaderTest {
    @TempDir Path temp;

    @Test
    void projectOverridesUserAndDeletionFallsBackAfterReload() throws Exception {
        Path project = Files.createDirectories(temp.resolve("project"));
        Path user = Files.createDirectories(temp.resolve("user"));
        Files.writeString(user.resolve("same.md"), skill("same", "用户版本", "user body"));
        Files.writeString(project.resolve("same.md"), skill("same", "项目版本", "project body"));

        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            SkillLoader loader = new SkillLoader(project, user, empty, new SkillParser());
            assertEquals(SkillOrigin.PROJECT, loader.snapshot().find("same").orElseThrow().origin());
            assertEquals("project body", loader.load("same", "").prompt());

            Files.delete(project.resolve("same.md"));
            assertEquals(SkillOrigin.USER, loader.reload().find("same").orElseThrow().origin());
            assertEquals("user body", loader.load("same", "").prompt());
        }
    }

    @Test
    void bundledCommitReviewAndTestUseSameParser() {
        SkillLoader loader = new SkillLoader(temp.resolve("project"), temp.resolve("user"),
                Thread.currentThread().getContextClassLoader(), new SkillParser());

        assertEquals(SkillMode.INLINE, loader.load("commit", "x").metadata().mode());
        assertEquals(SkillMode.FORK, loader.load("review", "x").metadata().mode());
        assertEquals(SkillMode.INLINE, loader.load("test", "x").metadata().mode());
        assertTrue(loader.snapshot().sorted().stream()
                .map(value -> value.metadata().name()).toList()
                .containsAll(java.util.List.of("commit", "review", "test")));
    }

    @Test
    void invalidRefreshKeepsPreviousSnapshotAndReportsDiagnostic() throws Exception {
        Path project = Files.createDirectories(temp.resolve("fallback-project"));
        Path user = Files.createDirectories(temp.resolve("fallback-user"));
        Path file = project.resolve("demo.md");
        Files.writeString(file, skill("demo", "有效版本", "stable"));
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            SkillLoader loader = new SkillLoader(project, user, empty, new SkillParser());
            long generation = loader.snapshot().generation();
            Files.writeString(project.resolve("help.md"), skill("help", "冲突", "invalid"));

            SkillCatalogSnapshot refreshed = loader.reload();

            assertEquals(generation, refreshed.generation());
            assertEquals("stable", loader.load("demo", "").prompt());
            assertTrue(refreshed.diagnostics().stream().anyMatch(value -> value.contains("冲突")));
        }
    }

    @Test
    void bodyEditIsReadFreshWithoutRestart() throws Exception {
        Path project = Files.createDirectories(temp.resolve("hot-project"));
        Path user = Files.createDirectories(temp.resolve("hot-user"));
        Path file = project.resolve("hot.md");
        Files.writeString(file, skill("hot", "热加载", "version-one"));
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            SkillLoader loader = new SkillLoader(project, user, empty, new SkillParser());
            assertEquals("version-one", loader.load("hot", "").prompt());
            Files.writeString(file, skill("hot", "热加载", "version-two"));
            assertEquals("version-two", loader.load("hot", "").prompt());
        }
    }

    @Test
    void metadataChangeAutomaticallyPublishesNewGeneration() throws Exception {
        Path project = Files.createDirectories(temp.resolve("meta-project"));
        Path user = Files.createDirectories(temp.resolve("meta-user"));
        Path file = project.resolve("meta.md");
        Files.writeString(file, skill("meta", "短描述", "body"));
        try (URLClassLoader empty = new URLClassLoader(new URL[0], null)) {
            SkillLoader loader = new SkillLoader(project, user, empty, new SkillParser());
            long generation = loader.snapshot().generation();
            Files.writeString(file, skill("meta", "明显更长的新描述", "body"));

            SkillCatalogSnapshot refreshed = loader.snapshot();

            assertTrue(refreshed.generation() > generation);
            assertEquals("明显更长的新描述",
                    refreshed.find("meta").orElseThrow().metadata().description());
        }
    }

    private static String skill(String name, String description, String body) {
        return "---\nname: " + name + "\ndescription: " + description + "\n---\n" + body + "\n";
    }
}
