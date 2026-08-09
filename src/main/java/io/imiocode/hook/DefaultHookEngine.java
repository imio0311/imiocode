package io.imiocode.hook;

import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.hook.action.ActionDispatcher;
import io.imiocode.hook.action.HookActionType;
import io.imiocode.hook.condition.ConditionEvaluator;
import io.imiocode.tool.SecretRedactor;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Hook 的匹配、once、同步/异步执行与工具拦截引擎。 */
public final class DefaultHookEngine implements HookRuntime {
    private static final int ASYNC_THREADS = 4;
    private final Map<HookEvent, List<Hook>> hooksByEvent;
    private final ConditionEvaluator conditions;
    private final ActionDispatcher actions;
    private final SecretRedactor redactor;
    private final Clock clock;
    private final HookPromptInbox prompts = new HookPromptInbox(128);
    private final HookNotificationQueue notifications;
    private final Set<String> fired = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor asyncExecutor;
    private final AtomicBoolean closed = new AtomicBoolean();

    public DefaultHookEngine(List<Hook> hooks, ConditionEvaluator conditions,
                             ActionDispatcher actions, SecretRedactor redactor, Clock clock) {
        this.conditions = conditions;
        this.actions = actions;
        this.redactor = redactor;
        this.clock = clock;
        this.notifications = new HookNotificationQueue(256, clock);
        EnumMap<HookEvent, List<Hook>> grouped = new EnumMap<>(HookEvent.class);
        for (HookEvent event : HookEvent.values()) grouped.put(event, new ArrayList<>());
        for (Hook hook : List.copyOf(hooks)) grouped.get(hook.event()).add(hook);
        grouped.replaceAll((event, values) -> List.copyOf(values));
        this.hooksByEvent = Map.copyOf(grouped);
        this.asyncExecutor = new ThreadPoolExecutor(ASYNC_THREADS, ASYNC_THREADS,
                30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128),
                Thread.ofVirtual().name("imiocode-hook-", 0).factory(),
                new ThreadPoolExecutor.AbortPolicy());
        asyncExecutor.allowCoreThreadTimeOut(true);
    }

    @Override public HookRunResult runHooks(HookContext context) {
        ensureOpen();
        if (context.event() == HookEvent.PRE_TOOL_USE)
            throw new IllegalArgumentException("pre_tool_use 必须调用 runPreToolHooks");
        List<HookExecutionRecord> records = new ArrayList<>();
        List<SystemReminder> reminders = new ArrayList<>();
        for (Hook hook : hooksByEvent.getOrDefault(context.event(), List.of())) {
            HookExecutionRecord skipped = skipIfNeeded(hook, context);
            if (skipped != null) { records.add(skipped); continue; }
            if (hook.async()) {
                HookExecutionRecord queued = queued(hook);
                records.add(queued); notify(queued);
                try { asyncExecutor.execute(() -> executeAsync(hook, context)); }
                catch (RejectedExecutionException exception) {
                    HookExecutionRecord failed = failedRecord(hook, "Hook 异步队列已满", Duration.ZERO);
                    records.add(failed); notify(failed);
                    if (hook.onError() == HookFailurePolicy.FAIL)
                        throw new HookExecutionException(hook.id(), safeFailure(failed));
                }
                continue;
            }
            HookExecutionRecord record = execute(hook, context);
            records.add(record); notify(record); collectPrompt(hook, record, reminders);
            if (!record.result().successful() && hook.onError() == HookFailurePolicy.FAIL)
                throw new HookExecutionException(hook.id(), safeFailure(record));
        }
        return new HookRunResult(records, reminders);
    }

    @Override public PreToolHookResult runPreToolHooks(HookContext context) {
        ensureOpen();
        if (context.event() != HookEvent.PRE_TOOL_USE)
            throw new IllegalArgumentException("runPreToolHooks 只接受 pre_tool_use");
        List<HookExecutionRecord> records = new ArrayList<>();
        for (Hook hook : hooksByEvent.getOrDefault(HookEvent.PRE_TOOL_USE, List.of())) {
            HookExecutionRecord skipped = skipIfNeeded(hook, context);
            if (skipped != null) { records.add(skipped); continue; }
            HookExecutionRecord record = execute(hook, context);
            records.add(record); notify(record);
            if (record.result().successful() && hook.action().type() == HookActionType.PROMPT && !hook.reject())
                offerPrompt(record.result().output());
            if (!record.result().successful()) {
                if (hook.onError() == HookFailurePolicy.REJECT)
                    return rejected(records, hook, safeFailure(record));
                if (hook.onError() == HookFailurePolicy.FAIL)
                    throw new HookExecutionException(hook.id(), safeFailure(record));
            }
            if (hook.reject()) {
                String reason = hook.rejectMessage().orElseGet(() -> {
                    String output = record.result().output().trim();
                    return output.isEmpty() ? "工具调用被 Hook 策略拒绝" : output;
                });
                return rejected(records, hook, redactor.redact(reason));
            }
        }
        return new PreToolHookResult(records, Optional.empty());
    }

    private HookExecutionRecord skipIfNeeded(Hook hook, HookContext context) {
        if (hook.condition().isPresent() && !conditions.matches(hook.condition().orElseThrow(), context))
            return new HookExecutionRecord(hook.id(), hook.event(), new HookActionResult(
                    HookExecutionStatus.SKIPPED_CONDITION, "", Optional.empty(), Duration.ZERO));
        if (hook.once() && !fired.add(hook.id()))
            return new HookExecutionRecord(hook.id(), hook.event(), new HookActionResult(
                    HookExecutionStatus.SKIPPED_ONCE, "", Optional.empty(), Duration.ZERO));
        return null;
    }

    private HookExecutionRecord execute(Hook hook, HookContext context) {
        try {
            return new HookExecutionRecord(hook.id(), hook.event(), sanitize(actions.execute(hook.action(), context)));
        } catch (RuntimeException exception) {
            return failedRecord(hook, safeException(exception), Duration.ZERO);
        }
    }

    private void executeAsync(Hook hook, HookContext context) {
        HookExecutionRecord record = execute(hook, context);
        notify(record); collectPrompt(hook, record, new ArrayList<>());
    }

    private void collectPrompt(Hook hook, HookExecutionRecord record, List<SystemReminder> resultReminders) {
        if (record.result().successful() && hook.action().type() == HookActionType.PROMPT) {
            SystemReminder reminder = offerPrompt(record.result().output());
            resultReminders.add(reminder);
        }
    }

    private SystemReminder offerPrompt(String content) {
        SystemReminder reminder = new SystemReminder(ReminderScope.ROUND, redactor.redact(content));
        prompts.offer(reminder); return reminder;
    }

    private PreToolHookResult rejected(List<HookExecutionRecord> records, Hook hook, String reason) {
        ToolRejectedError error = new ToolRejectedError(hook.id(), redactor.redact(reason));
        HookExecutionRecord rejection = new HookExecutionRecord(hook.id(), hook.event(),
                new HookActionResult(HookExecutionStatus.REJECTED, "", Optional.of(error.getMessage()), Duration.ZERO));
        records.add(rejection); notify(rejection);
        return new PreToolHookResult(records, Optional.of(error));
    }

    private HookActionResult sanitize(HookActionResult result) {
        return new HookActionResult(result.status(), redactor.redact(result.output()),
                result.safeError().map(redactor::redact), result.elapsed());
    }

    private void notify(HookExecutionRecord record) {
        String summary = record.result().safeError().orElseGet(() -> record.result().output().trim());
        if (summary.length() > 300) summary = summary.substring(0, 300) + "…";
        notifications.offer(new HookNotification(clock.instant(), record.hookId(), record.event(),
                record.result().status(), redactor.redact(summary)));
    }

    private static HookExecutionRecord queued(Hook hook) {
        return new HookExecutionRecord(hook.id(), hook.event(), new HookActionResult(
                HookExecutionStatus.QUEUED, "", Optional.empty(), Duration.ZERO));
    }
    private static HookExecutionRecord failedRecord(Hook hook, String message, Duration elapsed) {
        return new HookExecutionRecord(hook.id(), hook.event(), HookActionResult.failure(message, elapsed));
    }
    private String safeException(RuntimeException exception) {
        String message = exception.getMessage();
        return redactor.redact(message == null || message.isBlank() ? "Hook 执行失败" : message);
    }
    private static String safeFailure(HookExecutionRecord record) {
        return record.result().safeError().orElse("Hook 执行失败");
    }
    private void ensureOpen() {
        if (closed.get()) throw new IllegalStateException("Hook Engine 已关闭");
    }

    @Override public List<SystemReminder> drainPrompts() { return prompts.drain(); }
    @Override public List<HookNotification> drainNotifications() { return notifications.drain(); }
    @Override public void clearPrompts() { prompts.clear(); }
    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(2, TimeUnit.SECONDS)) asyncExecutor.shutdownNow();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); asyncExecutor.shutdownNow();
        } finally {
            actions.close(); prompts.clear();
        }
    }
}
