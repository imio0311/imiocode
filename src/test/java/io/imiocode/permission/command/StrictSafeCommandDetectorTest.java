package io.imiocode.permission.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrictSafeCommandDetectorTest {
    @TempDir
    Path workspace;

    @Test
    void recognizesReadOnlyCommandFamilies() throws IOException {
        Files.writeString(workspace.resolve("pom.xml"), "<project/>");
        StrictSafeCommandDetector detector = detector();

        Stream.of(
                "ls",
                "dir src",
                "pwd",
                "cat pom.xml",
                "Get-Content pom.xml",
                "head -n 5 pom.xml",
                "grep project pom.xml",
                "rg --files",
                "Select-String project pom.xml",
                "which git",
                "Get-Command java",
                "git status --short",
                "git diff",
                "git log -n 3",
                "git show HEAD",
                "git rev-parse HEAD",
                "git ls-files",
                "git remote -v",
                "git branch --show-current",
                "java -version",
                "mvn -version",
                "node --version",
                "go version",
                "whoami",
                "hostname",
                "date")
                .forEach(command -> assertSafe(detector, command));
    }

    @Test
    void rejectsCommandsWithSideEffectsOrExecutableHooks() {
        StrictSafeCommandDetector detector = detector();

        Stream.of(
                "git reset --hard",
                "git clean -fdx",
                "git checkout main",
                "git switch main",
                "git branch -D old",
                "git diff --output=result.patch",
                "git diff --ext-diff",
                "git show --textconv HEAD:file",
                "rg --pre cat pattern",
                "mvn test",
                "npm install",
                "curl https://example.com",
                "python script.py",
                "find . -delete")
                .forEach(command -> assertUncertain(detector, command));
    }

    @Test
    void requiresEverySegmentToBeSafe() {
        StrictSafeCommandDetector detector = detector();

        assertSafe(detector, "git status && java -version");
        assertSafe(detector, "cat 'a;b.txt' || Get-Date");
        assertUncertain(detector, "git status && rm -rf .");
        assertUncertain(detector, "git status\nrm -rf .");
        assertUncertain(detector, "git status | sh");
    }

    @Test
    void rejectsDynamicSyntaxAndWorkspaceEscape() {
        StrictSafeCommandDetector detector = detector();

        Stream.of(
                "cat ../secret.txt",
                "cat /etc/passwd",
                "cat C:\\Windows\\win.ini",
                "cat \\\\server\\share\\file",
                "cat $HOME/.ssh/config",
                "Get-Content $env:USERPROFILE\\secret",
                "cat file > copy",
                "echo $(whoami)",
                "git",
                "git remote",
                "git branch",
                "git status-malicious")
                .forEach(command -> assertUncertain(detector, command));
    }

    @Test
    void rejectsSymlinkThatEscapesWorkspaceWhenSupported() throws IOException {
        Path outside = Files.createTempDirectory("imiocode-safe-command-outside");
        Path link = workspace.resolve("outside-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException exception) {
            Files.deleteIfExists(outside);
            return;
        }
        try {
            assertUncertain(detector(), "cat outside-link/secret.txt");
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void matchesCommandNamesAsWholeTokensIgnoringCase() {
        StrictSafeCommandDetector detector = detector();

        assertSafe(detector, "GiT StAtUs");
        assertSafe(detector, "GET-DATE");
        assertUncertain(detector, "git-status");
        assertUncertain(detector, "catastrophe file");
    }

    private StrictSafeCommandDetector detector() {
        return new StrictSafeCommandDetector(workspace);
    }

    private static void assertSafe(StrictSafeCommandDetector detector, String command) {
        assertTrue(detector.inspect(command).safe(), command);
    }

    private static void assertUncertain(StrictSafeCommandDetector detector, String command) {
        assertFalse(detector.inspect(command).safe(), command);
    }
}
