package io.imiocode.tool.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BashToolTest {
    @TempDir
    Path workspace;

    @Test
    void runsInWorkspaceAndCapturesUtf8Output() {
        try (BashTool tool = tool(defaultLimits())) {
            ToolResult result = tool.execute(args(isWindows()
                    ? "[Console]::OutputEncoding=[Text.Encoding]::UTF8; Write-Output '中文'; (Get-Location).Path"
                    : "printf '中文\\n'; pwd"));

            assertTrue(result.success(), result.error());
            assertEquals(0, result.exitCode());
            assertTrue(result.output().contains("中文"));
            assertTrue(result.output().contains(workspace.getFileName().toString()));
        }
    }

    @Test
    void returnsNonZeroExitCodeAndTimesOut() {
        try (BashTool tool = tool(defaultLimits())) {
            ToolResult failed = tool.execute(args(isWindows() ? "exit 7" : "exit 7"));
            assertFalse(failed.success());
            assertEquals(7, failed.exitCode());
        }

        ToolLimits shortLimit = limits(Duration.ofMillis(150), 1024);
        try (BashTool tool = tool(shortLimit)) {
            ToolResult timeout = tool.execute(args(isWindows()
                    ? "Start-Sleep -Seconds 5"
                    : "sleep 5"));
            assertFalse(timeout.success());
            assertTrue(timeout.error().contains("超时"));
        }
    }

    @Test
    void truncatesOutput() {
        try (BashTool tool = tool(limits(Duration.ofSeconds(5), 64))) {
            ToolResult result = tool.execute(args(isWindows()
                    ? "[Console]::Out.Write(('x' * 500))"
                    : "printf '%0500d' 0"));
            assertTrue(result.success());
            assertTrue(result.truncated());
        }
    }

    @Test
    void removesSensitiveEnvironmentNamesAndSupportsCancellation() throws Exception {
        try (BashTool tool = tool(defaultLimits())) {
            ToolResult environment = tool.execute(args(isWindows()
                    ? "Get-ChildItem Env: | ForEach-Object { $_.Name }"
                    : "env | cut -d= -f1"));
            String names = environment.output().toUpperCase(Locale.ROOT);
            assertFalse(names.lines().anyMatch(name ->
                    name.contains("KEY") || name.contains("TOKEN") || name.contains("SECRET")
                            || name.contains("PASSWORD") || name.contains("CREDENTIAL")));

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var running = executor.submit(() -> tool.execute(args(isWindows()
                        ? "Start-Sleep -Seconds 10"
                        : "sleep 10")));
                Thread.sleep(200);
                tool.cancel();
                ToolResult cancelled = running.get(5, TimeUnit.SECONDS);
                assertFalse(cancelled.success());
            }
        }
    }

    private BashTool tool(ToolLimits limits) {
        return new BashTool(new WorkspacePolicy(workspace), limits, new SecretRedactor("test-secret"));
    }

    private static ToolLimits defaultLimits() {
        return limits(Duration.ofSeconds(5), 4096);
    }

    private static ToolLimits limits(Duration timeout, long outputBytes) {
        return new ToolLimits(
                1024, 100, 1024, 100, 100, 100, 100,
                8192, outputBytes, outputBytes, timeout, Duration.ofMillis(250));
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode args(String command) {
        return JsonNodeFactory.instance.objectNode().put("command", command);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
