package io.imiocode.subagent.task;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.subagent.runtime.SubagentRunResult;
import io.imiocode.subagent.runtime.SubagentRunner;
import io.imiocode.subagent.runtime.SubagentRunMode;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 有界后台任务表、通知队列与取消/接管入口。 */
public final class TaskManager implements AutoCloseable {
    private final SubagentRunner runner;
    private final int maxConcurrent, maxRecords, notificationCapacity;
    private final Clock clock;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("imio-subagent-timeout-", 0).factory());
    private final Map<String, MutableTask> tasks = new LinkedHashMap<>();
    private final ArrayDeque<TaskNotification> notifications = new ArrayDeque<>();
    private int running;

    public TaskManager(SubagentRunner runner, int maxConcurrent, int maxRecords, int notificationCapacity) {
        this(runner, maxConcurrent, maxRecords, notificationCapacity, Clock.systemUTC());
    }
    TaskManager(SubagentRunner runner, int maxConcurrent, int maxRecords, int notificationCapacity, Clock clock) {
        this.runner=runner; this.maxConcurrent=maxConcurrent; this.maxRecords=maxRecords;
        this.notificationCapacity=notificationCapacity; this.clock=clock;
    }

    public synchronized String submit(AgentDefinition definition, String description,
                                      List<ChatMessage> history) {
        return submit(definition, description, description, history, SubagentRunMode.DEFINITION);
    }

    public synchronized String submit(AgentDefinition definition, String description, String prompt,
                                      List<ChatMessage> history, SubagentRunMode mode) {
        return submitInternal(definition, description, prompt, history, true, mode);
    }

    public synchronized String submitForeground(AgentDefinition definition, String description,
                                                List<ChatMessage> history) {
        return submitForeground(definition, description, description, history, SubagentRunMode.DEFINITION);
    }

    public synchronized String submitForeground(AgentDefinition definition, String description, String prompt,
                                                List<ChatMessage> history, SubagentRunMode mode) {
        return submitInternal(definition, description, prompt, history, false, mode);
    }

    private String submitInternal(AgentDefinition definition, String description, String prompt,
                                  List<ChatMessage> history, boolean background, SubagentRunMode mode) {
        if (background && !definition.backgroundAllowed()) throw new IllegalArgumentException("Agent 不允许后台运行: " + definition.name());
        if (running >= maxConcurrent) throw new IllegalStateException("后台子 Agent 已达到并发上限 " + maxConcurrent);
        prune();
        String id = UUID.randomUUID().toString().substring(0, 8);
        MutableTask task = new MutableTask(id, definition, description, prompt, List.copyOf(history),
                clock.instant(), background, mode);
        tasks.put(id, task); running++;
        task.future = CompletableFuture.runAsync(() -> execute(task), executor);
        // Agent 自身先有同一 deadline；这里稍后兜底，避免正常 TIMEOUT 被误记为 CANCELLED。
        timer.schedule(() -> timeout(task), definition.timeout().toMillis() + 100L, TimeUnit.MILLISECONDS);
        return id;
    }

    public TaskSnapshot await(String id) throws InterruptedException {
        MutableTask task;
        synchronized (this) {
            task = tasks.get(id);
            if (task == null) throw new IllegalArgumentException("未知任务: " + id);
        }
        task.done.await();
        return find(id).orElseThrow();
    }

    public synchronized List<TaskSnapshot> list() {
        return tasks.values().stream().map(this::snapshot)
                .sorted(Comparator.comparing(TaskSnapshot::createdAt).reversed()).toList();
    }
    public synchronized Optional<TaskSnapshot> find(String id) {
        return Optional.ofNullable(tasks.get(id)).map(this::snapshot);
    }
    public synchronized boolean cancel(String id) {
        MutableTask task = tasks.get(id);
        if (task == null || terminal(task.status)) return false;
        task.cancelled.set(true); task.cancellation.get().run();
        if (task.future != null) task.future.cancel(true);
        finish(task, TaskStatus.CANCELLED, "任务已取消", null); return true;
    }
    public synchronized boolean detach(String id) {
        MutableTask task = tasks.get(id);
        if (task == null || terminal(task.status)) return false;
        task.detached = true;
        return true;
    }
    /** 等待后台任务终态并返回结果，UI 可把 ESC 后的任务切换为此前台观察。 */
    public TaskSnapshot adopt(String id) {
        CompletableFuture<Void> future;
        synchronized (this) {
            MutableTask task = tasks.get(id);
            if (task == null) throw new IllegalArgumentException("未知任务: " + id);
            future = task.future;
        }
        if (future != null) { try { future.join(); } catch (RuntimeException ignored) { } }
        return find(id).orElseThrow();
    }
    public synchronized List<TaskNotification> drainNotifications() {
        List<TaskNotification> result = new ArrayList<>(notifications); notifications.clear(); return List.copyOf(result);
    }

    private void execute(MutableTask task) {
        synchronized (this) { if (terminal(task.status)) return; task.status=TaskStatus.RUNNING; task.startedAt=clock.instant(); }
        SubagentRunResult result = runner.run(task.definition, task.prompt, task.history, task.background, task.mode,
                cancellation -> {
                    task.cancellation.set(cancellation);
                    if (task.cancelled.get()) cancellation.run();
                });
        synchronized (this) {
            if (terminal(task.status)) return;
            finish(task, result.success() ? TaskStatus.COMPLETED : TaskStatus.FAILED,
                    result.output(), result.traceId());
        }
    }
    private synchronized void timeout(MutableTask task) {
        if (terminal(task.status)) return;
        task.cancelled.set(true); task.cancellation.get().run();
        if (task.future != null) task.future.cancel(true);
        finish(task, TaskStatus.TIMED_OUT, "任务超时", null);
    }
    private void finish(MutableTask task, TaskStatus status, String output, String traceId) {
        if (terminal(task.status)) return;
        task.status=status; task.output=output; task.traceId=traceId; task.finishedAt=clock.instant(); running=Math.max(0,running-1);
        task.done.countDown();
        if (task.background || task.detached) {
            if (notifications.size() >= notificationCapacity) notifications.removeFirst();
            String summary = output == null ? status.name() : output.substring(0, Math.min(200, output.length()));
            notifications.addLast(new TaskNotification(task.id, status, summary, clock.instant()));
        }
    }
    private void prune() {
        while (tasks.size() >= maxRecords) {
            String removable = tasks.values().stream().filter(t -> terminal(t.status))
                    .min(Comparator.comparing(t -> t.createdAt)).map(t -> t.id).orElse(null);
            if (removable == null) throw new IllegalStateException("后台任务记录已满");
            tasks.remove(removable);
        }
    }
    private TaskSnapshot snapshot(MutableTask t) {
        return new TaskSnapshot(t.id,t.definition.name(),t.description,t.status,t.createdAt,
                Optional.ofNullable(t.startedAt),Optional.ofNullable(t.finishedAt),
                Optional.ofNullable(t.output),Optional.ofNullable(t.traceId));
    }
    private static boolean terminal(TaskStatus s) { return s==TaskStatus.COMPLETED||s==TaskStatus.FAILED||s==TaskStatus.CANCELLED||s==TaskStatus.TIMED_OUT; }
    @Override public void close() {
        List<String> ids;
        synchronized (this) { ids = new ArrayList<>(tasks.keySet()); }
        ids.forEach(this::cancel);
        executor.shutdownNow(); timer.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
            timer.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
    private static final class MutableTask {
        final String id; final AgentDefinition definition; final String description; final String prompt; final List<ChatMessage> history; final Instant createdAt;
        final SubagentRunMode mode;
        final boolean background; final CountDownLatch done=new CountDownLatch(1);
        final AtomicBoolean cancelled=new AtomicBoolean(); final AtomicReference<Runnable> cancellation=new AtomicReference<>(()->{});
        TaskStatus status=TaskStatus.PENDING; Instant startedAt,finishedAt; String output,traceId; CompletableFuture<Void> future; boolean detached;
        MutableTask(String id,AgentDefinition definition,String description,String prompt,List<ChatMessage> history,Instant createdAt,boolean background,SubagentRunMode mode){this.id=id;this.definition=definition;this.description=description;this.prompt=prompt;this.history=history;this.createdAt=createdAt;this.background=background;this.mode=mode;}
    }
}
