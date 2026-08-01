package io.imiocode.context;

import io.imiocode.llm.transport.MockLlmServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 手动验收用：先 package，再显式指定本测试，验证真实 shaded JAR。 */
class ShadedJarContextIT {
    @TempDir Path workspace;

    @Test
    void manualCompactPersistsSummaryIntoNextRequest() throws Exception {
        try (MockLlmServer server = new MockLlmServer();
             var readers = Executors.newVirtualThreadPerTaskExecutor()) {
            enqueueText(server, "已记住代号 ORBIT-42。");
            enqueueText(server, "<summary><prior_history>用户要求记住代号 ORBIT-42</prior_history>"
                    + "<active_task>第一轮已完成</active_task></summary>");
            enqueueText(server, "代号仍是 ORBIT-42。");

            Path jar = Path.of("target/imiocode-0.2.0-SNAPSHOT-all.jar").toAbsolutePath();
            String java = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                    .toString();
            ProcessBuilder builder = new ProcessBuilder(java, "-jar", jar.toString());
            builder.directory(workspace.toFile());
            builder.redirectErrorStream(true);
            builder.environment().put("IMIO_PROVIDER", "deepseek");
            builder.environment().put("IMIO_MODEL", "deepseek-chat");
            builder.environment().put("DEEPSEEK_API_KEY", "local-e2e-key");
            builder.environment().put("DEEPSEEK_BASE_URL", server.baseUri().toString());

            Process process = builder.start();
            try {
                var outputFuture = readers.submit(() -> new String(
                        process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                try (OutputStreamWriter input = new OutputStreamWriter(
                        process.getOutputStream(), StandardCharsets.UTF_8)) {
                    input.write("请记住代号 ORBIT-42。背景资料：" + "x".repeat(8_000) + "\n");
                    input.write("/compact\n");
                    input.write("刚才的代号是什么？\n");
                    input.write("/exit\n");
                    input.flush();
                }
                assertTrue(process.waitFor(30, TimeUnit.SECONDS), "真实 JAR 未在时限内退出");
                String output = outputFuture.get(2, TimeUnit.SECONDS);
                assertEquals(0, process.exitValue(), output);
                assertTrue(output.contains("[上下文] 手动压缩完成"), output);
                assertTrue(output.contains("代号仍是 ORBIT-42"), output);
                server.takeRequest();
                server.takeRequest();
                String followUp = server.takeRequest().body();
                assertTrue(followUp.contains("Compacted conversation summary"), followUp);
                assertTrue(followUp.contains("ORBIT-42"), followUp);
            } finally {
                if (process.isAlive()) process.destroyForcibly();
            }
        }
    }

    @Test
    void autoCompactRunsBeforeModelCallInRealJar() throws Exception {
        try (MockLlmServer server = new MockLlmServer();
             var readers = Executors.newVirtualThreadPerTaskExecutor()) {
            enqueueText(server, "<summary><prior_history>尚无已提交历史</prior_history>"
                    + "<active_task>用户提交了一段很长的测试任务</active_task></summary>");
            enqueueText(server, "自动压缩后任务完成。");
            ProcessBuilder builder = jarProcess(server);
            builder.environment().put("IMIO_MAX_OUTPUT_TOKENS", "100");
            builder.environment().put("IMIO_CONTEXT_WINDOW_TOKENS", "3000");
            builder.environment().put("IMIO_CONTEXT_AUTO_COMPACT_THRESHOLD", "0.50");
            Process process = builder.start();
            try {
                var outputFuture = readers.submit(() -> new String(
                        process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                try (OutputStreamWriter input = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                    input.write("执行长任务：" + "x".repeat(8_000) + "\n/exit\n");
                    input.flush();
                }
                assertTrue(process.waitFor(30, TimeUnit.SECONDS));
                String output = outputFuture.get(2, TimeUnit.SECONDS);
                assertEquals(0, process.exitValue(), output);
                assertTrue(output.contains("[上下文] 正在压缩"), output);
                assertTrue(output.contains("[上下文] 压缩完成"), output);
                assertTrue(output.contains("自动压缩后任务完成"), output);
                server.takeRequest();
                assertTrue(server.takeRequest().body().contains("Compacted conversation summary"));
            } finally {
                if (process.isAlive()) process.destroyForcibly();
            }
        }
    }

    @Test
    void largeToolResultSpillsToDiskInRealJar() throws Exception {
        Files.writeString(workspace.resolve("big.txt"), "大".repeat(7_000), StandardCharsets.UTF_8);
        try (MockLlmServer server = new MockLlmServer();
             var readers = Executors.newVirtualThreadPerTaskExecutor()) {
            server.enqueueSse("data: {\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,"
                    + "\"id\":\"call_big\",\"function\":{\"name\":\"read_file\","
                    + "\"arguments\":\"{\\\"path\\\":\\\"big.txt\\\"}\"}}]},"
                    + "\"finish_reason\":\"tool_calls\"}]}\n\ndata: [DONE]\n\n");
            enqueueText(server, "大文件读取完成。");
            Process process = jarProcess(server).start();
            try {
                var outputFuture = readers.submit(() -> new String(
                        process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                try (OutputStreamWriter input = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                    input.write("读取 big.txt。\n/exit\n");
                    input.flush();
                }
                assertTrue(process.waitFor(30, TimeUnit.SECONDS));
                String output = outputFuture.get(2, TimeUnit.SECONDS);
                assertEquals(0, process.exitValue(), output);
                assertTrue(output.contains("[上下文] 已落盘 1 个大结果"), output);
                assertTrue(output.contains("大文件读取完成"), output);
                Path resultDirectory = workspace.resolve(".imiocode/tool-results");
                assertTrue(Files.isDirectory(resultDirectory));
                assertEquals(1, Files.list(resultDirectory).count());
                assertTrue(Files.readString(Files.list(resultDirectory).findFirst().orElseThrow()).contains("大".repeat(100)));
            } finally {
                if (process.isAlive()) process.destroyForcibly();
            }
        }
    }

    private ProcessBuilder jarProcess(MockLlmServer server) {
        Path jar = Path.of("target/imiocode-0.2.0-SNAPSHOT-all.jar").toAbsolutePath();
        String java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
                .toString();
        ProcessBuilder builder = new ProcessBuilder(java, "-jar", jar.toString());
        builder.directory(workspace.toFile());
        builder.redirectErrorStream(true);
        builder.environment().put("IMIO_PROVIDER", "deepseek");
        builder.environment().put("IMIO_MODEL", "deepseek-chat");
        builder.environment().put("DEEPSEEK_API_KEY", "local-e2e-key");
        builder.environment().put("DEEPSEEK_BASE_URL", server.baseUri().toString());
        return builder;
    }

    private static void enqueueText(MockLlmServer server, String text) {
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        server.enqueueSse("data: {\"choices\":[{\"delta\":{\"content\":\"" + escaped
                + "\"},\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n");
    }
}
