package io.imiocode.hook.action;

import io.imiocode.hook.HookContext;
import io.imiocode.hook.HookEvent;
import io.imiocode.hook.HookExecutionStatus;
import io.imiocode.hook.template.HookTemplateResolver;
import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HookExecutorTest {
    @Test void commandReceivesMinimalContextAndRedactsOutput() {
        AtomicReference<Map<String, String>> captured = new AtomicReference<>();
        HookProcessRunner runner = new HookProcessRunner() {
            @Override public ProcessResult run(String command, Path workspace,
                                               Map<String, String> environment, Duration timeout) {
                captured.set(environment);
                return new ProcessResult(true, false, 0, "token-value", "", false, Duration.ZERO);
            }
            @Override public void close() { }
        };
        CommandHookExecutor executor = new CommandHookExecutor(new HookTemplateResolver(), runner,
                new SecretRedactor("token-value"));
        var result = executor.execute(new CommandAction("echo $EVENT", Duration.ofSeconds(1)),
                HookContext.builder(HookEvent.COMMAND_EXECUTE, Path.of("."))
                        .toolName("bash").toolArgs(Map.of("command", "echo ok")).build());
        assertEquals("***", result.output());
        assertEquals("command_execute", captured.get().get("MEWCODE_EVENT"));
        assertFalse(captured.get().keySet().stream().anyMatch(name -> name.contains("KEY")));
    }

    @Test void httpBuildsDefaultSafeJsonAndHandlesStatus() {
        AtomicReference<HookHttpRequest> captured = new AtomicReference<>();
        HookHttpTransport transport = request -> {
            captured.set(request);
            return new HookHttpResult(true, false, false, 204, "", "", Duration.ofMillis(1));
        };
        HttpHookExecutor executor = new HttpHookExecutor(new HookTemplateResolver(), transport,
                new SecretRedactor("secret"));
        var action = new HttpAction(URI.create("https://example.test/$EVENT"), "POST",
                Map.of("X-Tool", "$TOOL_NAME"), Optional.empty(), Duration.ofSeconds(1));
        var result = executor.execute(action, HookContext.builder(HookEvent.POST_TOOL_USE, Path.of("."))
                .toolName("grep").message("secret").build());
        assertTrue(result.successful());
        assertEquals("grep", captured.get().headers().get("X-Tool"));
        assertTrue(captured.get().body().contains("***"));
        assertFalse(captured.get().body().contains("secret"));
    }

    @Test void httpTooLargeAndAgentPlaceholderAreExplicitFailures() {
        HookHttpTransport transport = request -> new HookHttpResult(
                true, false, true, 200, "preview", "", Duration.ZERO);
        HttpHookExecutor executor = new HttpHookExecutor(new HookTemplateResolver(), transport,
                new SecretRedactor(""));
        var result = executor.execute(new HttpAction(URI.create("https://example.test"), "POST",
                        Map.of(), Optional.of("{}"), Duration.ofSeconds(1)),
                HookContext.builder(HookEvent.STARTUP, Path.of(".")).build());
        assertEquals(HookExecutionStatus.FAILED, result.status());
        assertEquals(HookExecutionStatus.NOT_IMPLEMENTED,
                new AgentPlaceholderHookExecutor().execute(new AgentAction("work"),
                        HookContext.builder(HookEvent.TURN_START, Path.of(".")).build()).status());
    }
}
