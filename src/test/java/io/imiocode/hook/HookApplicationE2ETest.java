package io.imiocode.hook;

import io.imiocode.llm.transport.MockLlmServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Windows 上用真实交互子进程替代 tmux，覆盖完整 UI → Agent → Hook 链路。 */
class HookApplicationE2ETest {
    @TempDir Path userHome;

    @Test void promptRejectFileChangeAndShutdownWorkThroughRealApplication() throws Exception {
        Path workspace = Files.createDirectory(userHome.resolve("hook-e2e"));
        Files.writeString(workspace.resolve("config.yaml"), config(), StandardCharsets.UTF_8);
        try (MockLlmServer server = new MockLlmServer();
             var reader = Executors.newVirtualThreadPerTaskExecutor()) {
            enqueueWrite(server, "reject-call", ".env", "SECRET=bad");
            enqueueText(server, "敏感文件写入已取消。");
            enqueueWrite(server, "safe-call", "safe.txt", "safe");
            enqueueText(server, "普通文件写入完成。");

            ProcessBuilder builder = applicationProcess(workspace, server);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            try {
                var output = reader.submit(() -> new String(
                        process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                try (OutputStreamWriter input = new OutputStreamWriter(
                        process.getOutputStream(), StandardCharsets.UTF_8)) {
                    input.write("请写入敏感文件。\n");
                    input.write("请写入普通文件。\n");
                    input.write("/exit\n");
                    input.flush();
                }
                assertTrue(process.waitFor(25, TimeUnit.SECONDS), "应用未按时退出");
                String terminal = output.get(2, TimeUnit.SECONDS);
                assertEquals(0, process.exitValue(), terminal);
                assertTrue(terminal.contains("敏感文件写入已取消"), terminal);
                assertTrue(terminal.contains("普通文件写入完成"), terminal);
            } finally {
                if (process.isAlive()) process.destroyForcibly();
            }

            String firstRequest = server.takeRequest().body();
            String rejectResultRequest = server.takeRequest().body();
            server.takeRequest();
            String safeResultRequest = server.takeRequest().body();
            assertTrue(firstRequest.contains("HOOK_PROMPT_ACTIVE"), firstRequest);
            assertTrue(rejectResultRequest.contains("blocked by hook protect-dotenv"), rejectResultRequest);
            assertTrue(safeResultRequest.contains("safe-call"), safeResultRequest);
            assertFalse(Files.exists(workspace.resolve(".env")));
            assertEquals("safe", Files.readString(workspace.resolve("safe.txt")));
            assertEquals(1, Files.readAllLines(workspace.resolve("file-hook.log")).size());
            String lifecycle = Files.readString(workspace.resolve("lifecycle.log"));
            assertTrue(lifecycle.contains("startup"), lifecycle);
            assertTrue(lifecycle.contains("session-start"), lifecycle);
            assertTrue(lifecycle.contains("session-end"), lifecycle);
            assertTrue(lifecycle.contains("shutdown"), lifecycle);
            assertTrue(lifecycle.indexOf("session-end") < lifecycle.indexOf("shutdown"), lifecycle);
        }
    }

    @Test void invalidHookConfigurationFallsBackToEmptyRuntimeAndStillOpensUi() throws Exception {
        Path workspace = Files.createDirectory(userHome.resolve("invalid-hook-e2e"));
        Files.writeString(workspace.resolve("config.yaml"), """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: test-key
                hooks:
                  - id: invalid
                    event: not-an-event
                    action:
                      type: prompt
                      message: ignored
                """, StandardCharsets.UTF_8);
        ProcessBuilder builder = applicationProcess(workspace, null);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        try (OutputStreamWriter input = new OutputStreamWriter(
                process.getOutputStream(), StandardCharsets.UTF_8)) {
            input.write("/exit\n");
            input.flush();
        }
        assertTrue(process.waitFor(15, TimeUnit.SECONDS));
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("[Hook/invalid]"), output);
    }

    private ProcessBuilder applicationProcess(Path workspace, MockLlmServer server) {
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        ProcessBuilder builder = new ProcessBuilder(java, "-Duser.home=" + userHome,
                "-cp", classpath, "io.imiocode.ImioCodeApplication");
        builder.directory(workspace.toFile());
        builder.environment().put("IMIO_PROVIDER", "deepseek");
        builder.environment().put("IMIO_MODEL", "deepseek-chat");
        builder.environment().put("DEEPSEEK_API_KEY", "local-hook-key");
        if (server != null) builder.environment().put("DEEPSEEK_BASE_URL", server.baseUri().toString());
        return builder;
    }

    private static String config() {
        return """
                provider: deepseek
                model: deepseek-chat
                providers:
                  deepseek:
                    api-key: ${DEEPSEEK_API_KEY}
                    base-url: ${DEEPSEEK_BASE_URL}
                hooks:
                  - id: startup-log
                    event: startup
                    action:
                      type: command
                      command: echo startup>>lifecycle.log
                  - id: session-start-log
                    event: session_start
                    action:
                      type: command
                      command: echo session-start>>lifecycle.log
                  - id: inject-turn
                    event: turn_start
                    action:
                      type: prompt
                      message: HOOK_PROMPT_ACTIVE
                  - id: protect-dotenv
                    event: pre_tool_use
                    if: 'tool_name == "write_file" && args.path == ".env"'
                    reject: true
                    reject-message: do not write secrets
                    action:
                      type: prompt
                      message: sensitive target
                  - id: file-change-log
                    event: file_change
                    action:
                      type: command
                      command: echo changed>>file-hook.log
                  - id: session-end-log
                    event: session_end
                    action:
                      type: command
                      command: echo session-end>>lifecycle.log
                  - id: shutdown-log
                    event: shutdown
                    action:
                      type: command
                      command: echo shutdown>>lifecycle.log
                """;
    }

    private static void enqueueWrite(MockLlmServer server, String id, String path, String content) {
        server.enqueueSse("""
                data: {"choices":[{"delta":{"tool_calls":[{"index":0,"id":"%s","function":{"name":"write_file","arguments":"{\\"path\\":\\"%s\\",\\"content\\":\\"%s\\"}"}}]},"finish_reason":"tool_calls"}]}

                data: [DONE]

                """.formatted(id, path, content));
    }

    private static void enqueueText(MockLlmServer server, String text) {
        server.enqueueSse("""
                data: {"choices":[{"delta":{"content":"%s"},"finish_reason":"stop"}]}

                data: [DONE]

                """.formatted(text));
    }
}
