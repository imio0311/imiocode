package io.imiocode;

import io.imiocode.llm.transport.MockLlmServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ch10ApplicationIT {
    @TempDir Path root;

    @Test
    void localCommandsBypassProviderAndRemainInteractive() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "local");
            String output = runApplication(workspace, home("local"), """
                    /help
                    /status
                    /permission full-access
                    /permission
                    /clear
                    /plan
                    /do
                    /session current
                    /memory list
                    /verbose
                    /compact-ui
                    /exit
                    """);

            assertEquals(0, server.requestCount(), output);
            assertTrue(output.contains("核心命令："), output);
            assertTrue(output.contains("Provider: deepseek"), output);
            assertTrue(output.contains("当前模式: full-access"), output);
            assertTrue(output.contains("--- 已清屏 ---"), output);
            assertTrue(output.contains("[模式] Plan"), output);
            assertTrue(output.contains("[模式] Do"), output);
            try (var archives = Files.list(workspace.resolve(".imiocode/sessions"))) {
                for (Path archive : archives.toList()) {
                    String jsonl = Files.readString(archive, StandardCharsets.UTF_8);
                    assertFalse(jsonl.contains("/permission full-access"), jsonl);
                    assertFalse(jsonl.contains("/status"), jsonl);
                }
            }
        }
    }

    @Test
    void reviewForwardsGeneratedPromptExactlyOnce() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "review");
            server.enqueueSse(answer("审查完成-MARKER"));

            String output = runApplication(workspace, home("review"),
                    "/review 重点关注并发安全\n/exit\n");

            assertTrue(output.contains("审查完成-MARKER"), output);
            assertEquals(1, server.requestCount());
            String request = server.takeRequest().body();
            assertTrue(request.contains("只做审查，不要修改文件"), request);
            assertTrue(request.contains("Additional focus"), request);
            assertTrue(request.contains("重点关注并发安全"), request);
            assertFalse(request.contains("/review"), request);
            Path archive = firstSessionArchive(workspace);
            String jsonl = Files.readString(archive, StandardCharsets.UTF_8);
            assertTrue(jsonl.contains("Additional focus"), jsonl);
            assertFalse(jsonl.contains("/review"), jsonl);
        }
    }

    @Test
    void compactUsesDedicatedSummaryRequestWithoutTools() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "compact");
            server.enqueueSse(answer("普通回复-MARKER"));
            server.enqueueSse(answer("<summary><prior_history>已讨论测试</prior_history><active_task>继续验证</active_task></summary>"));
            String longContext = "CH10-COMPACT-MARKER-" + "很长的上下文".repeat(2_000);

            String output = runApplication(workspace, home("compact"),
                    longContext + "\n/compact\n/exit\n");

            assertTrue(output.contains("普通回复-MARKER"), output);
            assertTrue(output.contains("手动压缩完成"), output);
            assertEquals(2, server.requestCount());
            server.takeRequest();
            String summaryRequest = server.takeRequest().body();
            assertTrue(summaryRequest.contains("上下文压缩器"), summaryRequest);
            assertTrue(summaryRequest.contains("CH10-COMPACT-MARKER"), summaryRequest);
            assertFalse(summaryRequest.contains("read_file"), summaryRequest);
            String jsonl = Files.readString(firstSessionArchive(workspace), StandardCharsets.UTF_8);
            assertTrue(jsonl.contains("REPLACE"), jsonl);
            assertTrue(jsonl.contains("Compacted conversation summary"), jsonl);
        }
    }

    @Test
    void runtimePermissionResetsAfterRestart() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "restart");
            Path home = home("restart");

            String first = runApplication(workspace, home, "/permission full-access\n/quit\n");
            String second = runApplication(workspace, home, "/permission\n/exit\n");

            assertTrue(first.contains("当前模式: full-access"), first);
            assertTrue(second.contains("当前模式: ask"), second);
            assertEquals(0, server.requestCount());
        }
    }

    @Test
    void commandErrorsStayLocalAndNextMessageStillWorks() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "recover");
            server.enqueueSse(answer("错误后恢复-MARKER"));

            String output = runApplication(workspace, home("recover"),
                    "/missing\n/permission ask extra\n正常消息\n/exit\n");

            assertTrue(output.contains("未知命令 /missing"), output);
            assertTrue(output.contains("用法：/permission"), output);
            assertTrue(output.contains("错误后恢复-MARKER"), output);
            assertEquals(1, server.requestCount());
            String request = server.takeRequest().body();
            assertTrue(request.contains("正常消息"), request);
            assertFalse(request.contains("/missing"), request);
            assertFalse(request.contains("/permission ask extra"), request);
        }
    }

    private Path workspace(MockLlmServer server, String name) throws Exception {
        Path workspace = Files.createDirectories(root.resolve("workspace-" + name));
        Files.createDirectories(workspace.resolve(".imiocode"));
        Files.writeString(workspace.resolve("config.yaml"), config(server), StandardCharsets.UTF_8);
        return workspace;
    }

    private static Path firstSessionArchive(Path workspace) throws Exception {
        try (var files = Files.list(workspace.resolve(".imiocode/sessions"))) {
            return files.filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .findFirst().orElseThrow();
        }
    }

    private Path home(String name) throws Exception {
        Path home = Files.createDirectories(root.resolve("home-" + name));
        Files.createDirectories(home.resolve(".imiocode"));
        return home;
    }

    private String runApplication(Path workspace, Path userHome, String input) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin",
                isWindows() ? "java.exe" : "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        ProcessBuilder builder = new ProcessBuilder(java, "-Duser.home=" + userHome,
                "-cp", classpath, ImioCodeApplication.class.getName());
        builder.directory(workspace.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        var reader = Executors.newSingleThreadExecutor();
        try {
            var outputFuture = reader.submit(() ->
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(input);
                writer.flush();
            }
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "ImioCode 子进程未按时退出");
            String output = outputFuture.get(2, TimeUnit.SECONDS);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            reader.shutdownNow();
        }
    }

    private String config(MockLlmServer server) {
        return """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: local-test-key
                    base-url: %s
                thinking:
                  enabled: false
                ui:
                  verbosity: compact
                memory:
                  auto-extract: false
                mcp:
                  servers: {}
                permissions:
                  mode: ask
                """.formatted(server.baseUri());
    }

    private static String answer(String text) {
        return "data: {\"choices\":[{\"delta\":{\"content\":\"" + text
                + "\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
