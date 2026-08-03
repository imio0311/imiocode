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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ch9ApplicationIT {
    private static final Pattern SESSION_ID = Pattern.compile("\\[会话] ([a-f0-9]{24})");

    @TempDir Path root;

    @Test
    void persistsAndExplicitlyRestoresConversationAcrossProcesses() throws Exception {
        Path workspace = Files.createDirectories(root.resolve("workspace"));
        Path userHome = Files.createDirectories(root.resolve("home"));
        Files.createDirectories(userHome.resolve(".imiocode"));
        Files.createDirectories(workspace.resolve(".imiocode"));
        Files.writeString(workspace.resolve("MEWCODE.md"), "CH9-INSTRUCTION-MARKER：始终用中文", StandardCharsets.UTF_8);
        Files.writeString(userHome.resolve(".imiocode/memories.md"),
                "# ImioCode Memories\n\n- [m_111111111111] [preference] USER-MEMORY-MARKER\n",
                StandardCharsets.UTF_8);
        Files.writeString(workspace.resolve(".imiocode/memories.md"),
                "# ImioCode Memories\n\n- [m_222222222222] [project_fact] PROJECT-MEMORY-MARKER\n",
                StandardCharsets.UTF_8);

        try (MockLlmServer server = new MockLlmServer()) {
            Files.writeString(workspace.resolve("config.yaml"), config(server), StandardCharsets.UTF_8);
            server.enqueueSse(answer("首次回复-MARKER"));

            String firstOutput = runApplication(workspace, userHome,
                    "请说明项目规则\n/session current\n/memory list\n/exit\n");
            assertTrue(firstOutput.contains("首次回复-MARKER"), firstOutput);
            String id = extractSessionId(firstOutput);
            String firstRequest = server.takeRequest().body();
            assertTrue(firstRequest.contains("CH9-INSTRUCTION-MARKER"), firstRequest);
            assertTrue(firstRequest.contains("USER-MEMORY-MARKER"), firstRequest);
            assertTrue(firstRequest.contains("PROJECT-MEMORY-MARKER"), firstRequest);

            Path archive = workspace.resolve(".imiocode/sessions/" + id + ".jsonl");
            assertTrue(Files.isRegularFile(archive));
            String jsonl = Files.readString(archive, StandardCharsets.UTF_8);
            assertTrue(jsonl.contains("请说明项目规则"), jsonl);
            assertTrue(jsonl.contains("首次回复-MARKER"), jsonl);
            assertFalse(jsonl.contains("CH9-INSTRUCTION-MARKER"), jsonl);

            server.enqueueSse(answer("恢复成功-MARKER"));
            String secondOutput = runApplication(workspace, userHome,
                    "/session resume " + id + "\n继续上一轮\n/exit\n");
            assertTrue(secondOutput.contains("已恢复 " + id), secondOutput);
            assertTrue(secondOutput.contains("恢复成功-MARKER"), secondOutput);
            String secondRequest = server.takeRequest().body();
            assertTrue(secondRequest.contains("请说明项目规则"), secondRequest);
            assertTrue(secondRequest.contains("首次回复-MARKER"), secondRequest);
            assertTrue(secondRequest.contains("继续上一轮"), secondRequest);
        }
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

    private static String extractSessionId(String output) {
        var matcher = SESSION_ID.matcher(output);
        if (!matcher.find()) throw new AssertionError("输出中没有会话 ID：" + output);
        return matcher.group(1);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
