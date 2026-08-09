package io.imiocode.hook;

import io.imiocode.hook.action.*;
import io.imiocode.hook.condition.DefaultConditionEvaluator;
import io.imiocode.hook.template.HookTemplateResolver;
import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DefaultHookEngineTest {
    @Test void promptOnceRunsOnlyOnceAndDrainsExactlyOnce() {
        Hook hook = hook("remember", HookEvent.TURN_START, new PromptAction("event=$EVENT"), true, false, false);
        try (DefaultHookEngine engine = engine(List.of(hook), new AtomicInteger())) {
            HookContext context = context(HookEvent.TURN_START);
            assertEquals(HookExecutionStatus.SUCCEEDED, engine.runHooks(context).executions().getFirst().result().status());
            assertEquals(HookExecutionStatus.SKIPPED_ONCE, engine.runHooks(context).executions().getFirst().result().status());
            assertEquals(1, engine.drainPrompts().size());
            assertTrue(engine.drainPrompts().isEmpty());
        }
    }

    @Test void preToolRejectReturnsStableErrorAndStopsFollowingHooks() {
        AtomicInteger commands = new AtomicInteger();
        Hook reject = new Hook("protect-env", HookEvent.PRE_TOOL_USE, Optional.empty(),
                new PromptAction("不要修改密钥文件"), false, false, true,
                Optional.of("禁止写入 .env"), HookFailurePolicy.IGNORE);
        Hook later = hook("later", HookEvent.PRE_TOOL_USE,
                new CommandAction("ignored", Duration.ofSeconds(1)), false, false, false);
        try (DefaultHookEngine engine = engine(List.of(reject, later), commands)) {
            PreToolHookResult result = engine.runPreToolHooks(
                    HookContext.builder(HookEvent.PRE_TOOL_USE, Path.of("."))
                            .toolName("write_file").toolArgs(java.util.Map.of("path", ".env")).build());
            assertFalse(result.allowed());
            assertEquals("blocked by hook protect-env: 禁止写入 .env",
                    result.rejection().orElseThrow().getMessage());
            assertEquals(0, commands.get());
        }
    }

    @Test void ordinaryAndPreToolEntrypointsRejectWrongEvent() {
        try (DefaultHookEngine engine = engine(List.of(), new AtomicInteger())) {
            assertThrows(IllegalArgumentException.class,
                    () -> engine.runHooks(context(HookEvent.PRE_TOOL_USE)));
            assertThrows(IllegalArgumentException.class,
                    () -> engine.runPreToolHooks(context(HookEvent.TURN_START)));
        }
    }

    @Test void asyncReturnsBeforeActionCompletesAndPublishesCompletion() throws Exception {
        AtomicInteger commands = new AtomicInteger();
        Hook hook = hook("background", HookEvent.TURN_END,
                new CommandAction("slow", Duration.ofSeconds(2)), false, true, false);
        try (DefaultHookEngine engine = engine(List.of(hook), commands, 150)) {
            long start = System.nanoTime();
            HookRunResult result = engine.runHooks(context(HookEvent.TURN_END));
            assertEquals(HookExecutionStatus.QUEUED, result.executions().getFirst().result().status());
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 100);
            Thread.sleep(250);
            assertEquals(1, commands.get());
            assertTrue(engine.drainNotifications().stream()
                    .anyMatch(item -> item.status() == HookExecutionStatus.SUCCEEDED));
        }
    }

    @Test void onceReservationIsAtomicAcrossConcurrentTriggers() throws Exception {
        AtomicInteger commands = new AtomicInteger();
        Hook hook = hook("atomic-once", HookEvent.FILE_CHANGE,
                new CommandAction("run", Duration.ofSeconds(1)), true, false, false);
        try (DefaultHookEngine engine = engine(List.of(hook), commands, 20);
             var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            List<java.util.concurrent.Future<HookRunResult>> tasks = new java.util.ArrayList<>();
            for (int index = 0; index < 24; index++)
                tasks.add(workers.submit(() -> engine.runHooks(context(HookEvent.FILE_CHANGE))));
            for (var task : tasks) task.get();
            assertEquals(1, commands.get());
        }
    }

    private static DefaultHookEngine engine(List<Hook> hooks, AtomicInteger commands) {
        return engine(hooks, commands, 0);
    }

    private static DefaultHookEngine engine(List<Hook> hooks, AtomicInteger commands, long delayMillis) {
        HookActionExecutor<CommandAction> command = new HookActionExecutor<>() {
            @Override public HookActionType type() { return HookActionType.COMMAND; }
            @Override public HookActionResult execute(CommandAction action, HookContext context) {
                commands.incrementAndGet();
                if (delayMillis > 0) try { Thread.sleep(delayMillis); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return HookActionResult.success("ok", Duration.ZERO);
            }
        };
        HookActionExecutor<HttpAction> http = new HookActionExecutor<>() {
            @Override public HookActionType type() { return HookActionType.HTTP; }
            @Override public HookActionResult execute(HttpAction action, HookContext context) {
                return HookActionResult.success("ok", Duration.ZERO);
            }
        };
        ActionDispatcher dispatcher = new ActionDispatcher(List.of(command,
                new PromptHookExecutor(new HookTemplateResolver()), http,
                new AgentPlaceholderHookExecutor()));
        return new DefaultHookEngine(hooks, new DefaultConditionEvaluator(), dispatcher,
                new SecretRedactor("secret-value"), Clock.systemUTC());
    }

    private static Hook hook(String id, HookEvent event, Action action,
                             boolean once, boolean async, boolean reject) {
        return new Hook(id, event, Optional.empty(), action, once, async, reject,
                Optional.empty(), HookFailurePolicy.IGNORE);
    }

    private static HookContext context(HookEvent event) {
        return HookContext.builder(event, Path.of(".")).build();
    }
}
