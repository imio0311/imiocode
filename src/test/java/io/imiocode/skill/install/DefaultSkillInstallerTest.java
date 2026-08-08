package io.imiocode.skill.install;

import io.imiocode.skill.SkillLoader;
import io.imiocode.skill.SkillParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSkillInstallerTest {
    @TempDir Path temp;

    @Test
    void installsValidatedPackageAndReloadsCatalog() throws Exception {
        Path workspace = Files.createDirectories(temp.resolve("workspace"));
        SkillLoader loader = new SkillLoader(workspace, temp.resolve("user"));
        DefaultSkillInstaller installer = installer(workspace, loader, skill("remote-ui", "new body"), loader::reload);

        SkillInstallResult result = installer.install(request(false), SkillInstallListener.NOOP);

        assertEquals("remote-ui", result.skillName());
        assertTrue(Files.exists(workspace.resolve(".imiocode/skills/remote-ui/SKILL.md")));
        assertTrue(loader.snapshot().find("remote-ui").isPresent());
        assertEquals(List.of(SkillInstallStage.QUEUED, SkillInstallStage.DOWNLOADING,
                SkillInstallStage.VALIDATING, SkillInstallStage.INSTALLING,
                SkillInstallStage.RELOADING, SkillInstallStage.COMPLETED), result.stages());
        assertNoTransactionResidue(workspace);
    }

    @Test
    void rejectsExistingSkillWithoutForce() throws Exception {
        Path workspace = Files.createDirectories(temp.resolve("workspace"));
        Path existing = workspace.resolve(".imiocode/skills/remote-ui");
        Files.createDirectories(existing);
        Files.writeString(existing.resolve("SKILL.md"), skillText("remote-ui", "old body"));
        SkillLoader loader = new SkillLoader(workspace, temp.resolve("user"));
        DefaultSkillInstaller installer = installer(workspace, loader, skill("remote-ui", "new body"), loader::reload);

        assertThrows(SkillInstallException.class, () -> installer.install(request(false), SkillInstallListener.NOOP));
        assertTrue(Files.readString(existing.resolve("SKILL.md")).contains("old body"));
        assertNoTransactionResidue(workspace);
    }

    @Test
    void restoresOldVersionWhenReloadFailsDuringForce() throws Exception {
        Path workspace = Files.createDirectories(temp.resolve("workspace"));
        Path existing = workspace.resolve(".imiocode/skills/remote-ui");
        Files.createDirectories(existing);
        Files.writeString(existing.resolve("SKILL.md"), skillText("remote-ui", "old body"));
        SkillLoader loader = new SkillLoader(workspace, temp.resolve("user"));
        DefaultSkillInstaller installer = installer(workspace, loader, skill("remote-ui", "new body"),
                () -> { throw new SkillInstallException("reload failed"); });

        assertThrows(SkillInstallException.class, () -> installer.install(request(true), SkillInstallListener.NOOP));
        assertTrue(Files.readString(existing.resolve("SKILL.md")).contains("old body"));
        assertFalse(Files.readString(existing.resolve("SKILL.md")).contains("new body"));
        assertNoTransactionResidue(workspace);
    }

    private DefaultSkillInstaller installer(
            Path workspace, SkillLoader loader, RemoteSkillPackage remote, SkillInstallRefresher refresher) {
        SkillInstallConfig config = SkillInstallConfig.defaults();
        RemoteSkillFetcher fetcher = new RemoteSkillFetcher() {
            @Override public RemoteSkillPackage fetch(RemoteSkillLocation location, SkillDownloadBudget budget) {
                remote.files().forEach(file -> { budget.claimFile(); budget.consumeFileBytes(file.content().length); });
                return remote;
            }
            @Override public void cancel() { }
        };
        return new DefaultSkillInstaller(workspace, config, new RemoteSkillLocator(config), fetcher,
                new SkillParser(), loader, refresher);
    }

    private static SkillInstallRequest request(boolean force) {
        return new SkillInstallRequest(URI.create("https://skills.sh/acme/repo/remote-ui"), force);
    }

    private static RemoteSkillPackage skill(String name, String body) {
        return new RemoteSkillPackage(URI.create("https://skills.sh/acme/repo/" + name),
                List.of(new RemoteSkillFile(Path.of("SKILL.md"),
                        skillText(name, body).getBytes(StandardCharsets.UTF_8))));
    }

    private static String skillText(String name, String body) {
        return "---\nname: " + name + "\ndescription: test skill\nallowedTools: [read_file]\n---\n# Test\n" + body + "\n";
    }

    private static void assertNoTransactionResidue(Path workspace) throws Exception {
        Path root = workspace.resolve(".imiocode/skills");
        if (!Files.exists(root)) return;
        try (var files = Files.list(root)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith(".install-")
                    || path.getFileName().toString().startsWith(".backup-")));
        }
    }
}
