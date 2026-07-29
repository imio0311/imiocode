package io.imiocode.prompt;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentContextCollectorTest {

    @TempDir
    Path tempDir;

    @Test
    void capturesFixedTimeAndReturnsAClassifiedGitState() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-29T02:30:00Z"),
                ZoneId.of("Asia/Shanghai"));
        var collector = new EnvironmentContextCollector(
                tempDir, clock, Duration.ofSeconds(2), "deepseek-chat");

        EnvironmentContext context = collector.capture();

        assertEquals(tempDir.toAbsolutePath().normalize(), context.workspace());
        assertEquals(2026, context.capturedAt().getYear());
        assertEquals(10, context.capturedAt().getHour());
        assertNotNull(context.operatingSystem());
        assertNotNull(context.architecture());
        assertNotNull(context.shell());
        assertNotNull(context.git().state());
        assertEquals("deepseek-chat", context.model());
    }

    @Test
    void capturesBranchAndDirtyStateWithoutExposingFileName() throws Exception {
        Assumptions.assumeTrue(run(tempDir, "git", "init", "-b", "ch5-test") == 0,
                "当前环境没有可用的 Git");
        Files.writeString(tempDir.resolve("secret-file-name.txt"), "not committed");

        var collector = new EnvironmentContextCollector(
                tempDir,
                Clock.system(ZoneId.of("Asia/Shanghai")),
                Duration.ofSeconds(2));
        EnvironmentContext context = collector.capture();
        String reminder = new EnvironmentReminderFormatter().format(context).content();

        assertEquals("ch5-test", context.git().branch().orElseThrow());
        assertEquals(GitWorkingTreeState.DIRTY, context.git().state());
        assertTrue(!reminder.contains("secret-file-name.txt"));
    }

    @Test
    void rejectsNonPositiveGitTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> new EnvironmentContextCollector(
                        tempDir, Clock.systemUTC(), Duration.ZERO));
    }

    @Test
    void degradesToUnavailableWhenGitCannotInspectWorkspace() {
        Path missingWorkspace = tempDir.resolve("does-not-exist");
        var collector = new EnvironmentContextCollector(
                missingWorkspace,
                Clock.systemUTC(),
                Duration.ofMillis(200));

        EnvironmentContext context = collector.capture();

        assertEquals(GitWorkingTreeState.UNAVAILABLE, context.git().state());
        assertTrue(context.git().branch().isEmpty());
    }

    @Test
    void legacyConstructorUsesUnknownModelAndCapturesArchitecture() {
        var collector = new EnvironmentContextCollector(
                tempDir,
                Clock.systemUTC(),
                Duration.ofSeconds(2));

        EnvironmentContext context = collector.capture();

        assertEquals("unknown", context.model());
        assertTrue(!context.architecture().isBlank());
    }

    @Test
    void rejectsBlankModel() {
        assertThrows(IllegalArgumentException.class,
                () -> new EnvironmentContextCollector(
                        tempDir,
                        Clock.systemUTC(),
                        Duration.ofSeconds(2),
                        " "));
    }

    private static int run(Path directory, String... command)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            return -1;
        }
        return process.exitValue();
    }
}
