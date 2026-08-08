package io.imiocode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class Ch11ApplicationIT {
    @TempDir Path root;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void localSkillManagementBypassesModel() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "local");
            String output = runApplication(workspace, home("local"), """
                    /skill list
                    /skill info commit
                    /skill reload
                    /exit
                    """);
            assertEquals(0, server.requestCount(), output);
            assertTrue(output.contains("commit [builtin/inline]"), output);
            assertTrue(output.contains("review [builtin/fork]"), output);
            assertTrue(output.contains("test [builtin/inline]"), output);
            assertTrue(output.contains("Skill /commit"), output);
            assertTrue(output.contains("Skill 已刷新"), output);
        }
    }

    @Test
    void slashCommitInjectsFullSopAndArgumentsInOneRequest() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "commit");
            server.enqueueSse(answer("提交流程完成-CH11"));
            String output = runApplication(workspace, home("commit"), """
                    /commit 文档更新
                    /exit
                    """);
            assertTrue(output.contains("提交流程完成-CH11"), output);
            assertEquals(1, server.requestCount());
            String body = server.takeRequest().body();
            assertTrue(body.contains("Commit 工作流"), body);
            assertTrue(body.contains("文档更新"), body);
            assertFalse(body.contains("$ARGUMENTS"), body);
        }
    }

    @Test
    void naturalLanguageLoadsSummaryThenFullSopOnSecondRound() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "natural");
            server.enqueueSse(loadSkillCall("skill-1", "commit", "自然语言提交"));
            server.enqueueSse(answer("自然语言 Skill 完成-CH11"));
            String output = runApplication(workspace, home("natural"), """
                    帮我提交一下
                    /exit
                    """);
            assertTrue(output.contains("自然语言 Skill 完成-CH11"), output);
            assertEquals(2, server.requestCount());
            JsonNode first = json.readTree(server.takeRequest().body());
            JsonNode second = json.readTree(server.takeRequest().body());
            String firstText = first.toString();
            String secondText = second.toString();
            assertTrue(firstText.contains("可用 Skill"), firstText);
            assertFalse(firstText.contains("Commit 工作流"), firstText);
            assertTrue(secondText.contains("Commit 工作流"), secondText);
            assertTrue(secondText.contains("自然语言提交"), secondText);
            assertTrue(toolNames(second).contains("load_skill"), secondText);
            assertTrue(toolNames(second).contains("bash"), secondText);
            assertFalse(toolNames(second).contains("write_file"), secondText);
            assertFalse(toolNames(second).contains("edit_file"), secondText);
        }
    }

    @Test
    void reviewForkReturnsOnlyFinalResultToParentArchive() throws Exception {
        try (MockLlmServer server = new MockLlmServer()) {
            Path workspace = workspace(server, "fork");
            server.enqueueSse(answer("隔离审查完成-CH11"));
            String output = runApplication(workspace, home("fork"), """
                    /review 关注并发
                    /exit
                    """);
            assertTrue(output.contains("隔离审查完成-CH11"), output);
            assertEquals(1, server.requestCount());
            String request = server.takeRequest().body();
            assertTrue(request.contains("Review 工作流"), request);
            assertTrue(request.contains("关注并发"), request);
            String archive = Files.readString(firstSessionArchive(workspace), StandardCharsets.UTF_8);
            assertTrue(archive.contains("/review 关注并发"), archive);
            assertTrue(archive.contains("隔离审查完成-CH11"), archive);
            assertFalse(archive.contains("Review 工作流"), archive);
            assertFalse(archive.contains("tool_result"), archive);
        }
    }

    private static java.util.Set<String> toolNames(JsonNode request) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        request.path("tools").forEach(tool -> names.add(
                tool.path("function").path("name").asText()));
        return names;
    }

    private Path workspace(MockLlmServer server, String name) throws Exception {
        Path workspace = Files.createDirectories(root.resolve("workspace-" + name));
        Files.createDirectories(workspace.resolve(".imiocode"));
        Files.writeString(workspace.resolve("config.yaml"), config(server), StandardCharsets.UTF_8);
        return workspace;
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
            assertTrue(process.waitFor(25, TimeUnit.SECONDS), "ImioCode 子进程未按时退出");
            String output = outputFuture.get(2, TimeUnit.SECONDS);
            assertEquals(0, process.exitValue(), output);
            return output;
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            reader.shutdownNow();
        }
    }

    private static Path firstSessionArchive(Path workspace) throws Exception {
        try (var files = Files.list(workspace.resolve(".imiocode/sessions"))) {
            return files.filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .findFirst().orElseThrow();
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
                  mode: full-access
                """.formatted(server.baseUri());
    }

    private static String answer(String text) {
        return "data: {\"choices\":[{\"delta\":{\"content\":\"" + text
                + "\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
    }

    private static String loadSkillCall(String id, String name, String arguments) {
        return """
                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"%s","function":{"name":"load_skill","arguments":"{\\"name\\":\\"%s\\",\\"arguments\\":\\"%s\\"}"}}]},"finish_reason":"tool_calls"}]}

                data: [DONE]

                """.formatted(id, name, arguments);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("win");
    }
}
