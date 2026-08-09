package io.imiocode.subagent.runtime;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.subagent.definition.AgentDefinitionLoader;
import io.imiocode.subagent.definition.AgentIsolation;
import io.imiocode.subagent.task.TaskManager;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.Set;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import io.imiocode.subagent.task.TaskSnapshot;
import io.imiocode.subagent.task.TaskStatus;

/** 单一 agent 工具背后的同步与后台路由。 */
public final class SubagentDispatcher {
    private final AgentDefinitionLoader definitions;
    private final TaskManager tasks;
    private final Supplier<List<ChatMessage>> parentHistory;
    private final Path workspace;
    private final AtomicReference<ActiveSynchronous> activeSynchronous = new AtomicReference<>();

    public String catalogSummary() {
        return definitions.snapshot().definitions().values().stream()
                .sorted(java.util.Comparator.comparing(AgentDefinition::name))
                .map(definition -> definition.name() + ": " + definition.description())
                .collect(java.util.stream.Collectors.joining("; "));
    }

    public SubagentDispatcher(AgentDefinitionLoader definitions, SubagentRunner runner,
                              TaskManager tasks, Supplier<List<ChatMessage>> parentHistory) {
        this(definitions, runner, tasks, parentHistory, Path.of("").toAbsolutePath());
    }
    public SubagentDispatcher(AgentDefinitionLoader definitions, SubagentRunner runner,
                              TaskManager tasks, Supplier<List<ChatMessage>> parentHistory, Path workspace) {
        this.definitions=Objects.requireNonNull(definitions); Objects.requireNonNull(runner);
        this.tasks=Objects.requireNonNull(tasks); this.parentHistory=Objects.requireNonNull(parentHistory);
        this.workspace=Objects.requireNonNull(workspace).toAbsolutePath().normalize();
    }

    public SubagentDispatchResult dispatch(String type, String description, String prompt,
                                           boolean background, String isolation, String model,
                                           String cwd, Set<String> additionallyDeniedTools)
            throws InterruptedException {
        boolean fork = type == null || type.isBlank();
        AgentDefinition definition = definitions.snapshot().find(fork ? "general-purpose" : type)
                .orElseThrow(() -> new IllegalArgumentException("未知 subagent_type: " + type));
        SubagentRunMode mode = fork ? SubagentRunMode.FORK : SubagentRunMode.DEFINITION;
        if (model != null && !model.isBlank()) definition = definition.withModel(model.trim());
        definition = definition.withAdditionalDeniedTools(additionallyDeniedTools);
        AgentIsolation requested = isolation == null || isolation.isBlank()
                ? definition.isolation() : AgentIsolation.parse(isolation);
        if (requested != AgentIsolation.NONE) throw new IllegalArgumentException("CH13 暂不支持 worktree isolation");
        String relativeCwd = validateCwd(cwd);
        if (!".".equals(relativeCwd)) prompt = "任务工作目录为 " + relativeCwd
                + "；所有文件路径请以项目根为基准并带此前缀。\n\n" + prompt;
        List<ChatMessage> history = List.copyOf(parentHistory.get());
        if (background) {
            String id = tasks.submit(definition, description, prompt, history, mode);
            return new SubagentDispatchResult(true, "后台子 Agent 已启动，task_id=" + id, id, true);
        }
        String id = tasks.submitForeground(definition, description, prompt, history, mode);
        ActiveSynchronous active = new ActiveSynchronous(id, Thread.currentThread());
        if (!activeSynchronous.compareAndSet(null, active)) {
            tasks.cancel(id);
            throw new IllegalStateException("已有同步子 Agent 正在执行");
        }
        try {
            TaskSnapshot result = tasks.await(id);
            boolean success = result.status() == TaskStatus.COMPLETED;
            return new SubagentDispatchResult(success, result.output().orElse("子 Agent 未返回结果"),
                    result.traceId().orElse(id), false);
        } finally {
            activeSynchronous.compareAndSet(active, null);
        }
    }

    private String validateCwd(String cwd) {
        if (cwd == null || cwd.isBlank() || ".".equals(cwd.trim())) return ".";
        Path requested = Path.of(cwd.trim());
        Path resolved = (requested.isAbsolute() ? requested : workspace.resolve(requested)).normalize();
        if (!resolved.startsWith(workspace)) throw new IllegalArgumentException("cwd 必须位于当前工作区内");
        try {
            Path existing = java.nio.file.Files.exists(resolved) ? resolved.toRealPath() : resolved;
            Path realWorkspace = workspace.toRealPath();
            if (!existing.startsWith(realWorkspace)) throw new IllegalArgumentException("cwd 符号链接越界");
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("cwd 无法安全验证");
        }
        return workspace.relativize(resolved).toString().replace('\\','/');
    }

    public void cancelActiveSynchronous() {
        ActiveSynchronous active = activeSynchronous.get();
        if (active != null) {
            tasks.detach(active.taskId());
            active.waiter().interrupt();
        }
    }

    private record ActiveSynchronous(String taskId, Thread waiter) { }
}
