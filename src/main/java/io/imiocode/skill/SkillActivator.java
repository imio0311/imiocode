package io.imiocode.skill;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.agent.AgentEventListener;
import io.imiocode.conversation.ReminderScope;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolSelection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** 维护任务级 activeSkills、完整 SOP 和临时专属工具。 */
public final class SkillActivator {
    public static final String LOAD_SKILL_TOOL = "load_skill";
    public static final String INSTALL_SKILL_TOOL = "install_skill";
    private static final Set<String> SYSTEM_TOOLS = Set.of(LOAD_SKILL_TOOL, INSTALL_SKILL_TOOL);

    private final ToolRegistry registry;
    private final Function<SkillToolSpec, ? extends Tool> toolFactory;
    private final InheritableThreadLocal<Deque<RunState>> states =
            new InheritableThreadLocal<>() {
                @Override protected Deque<RunState> initialValue() { return new ArrayDeque<>(); }
                @Override protected Deque<RunState> childValue(Deque<RunState> parent) {
                    return new ArrayDeque<>(parent);
                }
            };

    public SkillActivator(ToolRegistry registry,
                          Function<SkillToolSpec, ? extends Tool> toolFactory) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.toolFactory = Objects.requireNonNull(toolFactory, "toolFactory");
    }

    public SkillRunScope beginRun(Optional<SkillInvocation> initial,
                                  List<ChatMessage> sourceHistory) {
        return beginRun(initial, sourceHistory, AgentEventListener.NOOP);
    }

    public SkillRunScope beginRun(Optional<SkillInvocation> initial,
                                  List<ChatMessage> sourceHistory,
                                  AgentEventListener listener) {
        Deque<RunState> stack = states.get();
        RunState state = new RunState(sourceHistory,
                Objects.requireNonNullElse(listener, AgentEventListener.NOOP));
        stack.push(state);
        try {
            initial.ifPresent(invocation -> activate(invocation.skill()));
        } catch (RuntimeException exception) {
            stack.pop();
            throw exception;
        }
        return () -> endRun(stack, state);
    }

    public void validate(LoadedSkill skill) {
        Objects.requireNonNull(skill, "skill");
        Set<String> dedicated = new LinkedHashSet<>();
        skill.tools().forEach(tool -> dedicated.add(tool.exposedName()));
        List<String> missing = skill.allowedTools().stream()
                .filter(name -> !dedicated.contains(name) && !registry.isEnabled(name))
                .sorted().toList();
        if (!missing.isEmpty()) {
            throw new SkillException("Skill " + skill.metadata().name()
                    + " 依赖的工具不可用: " + String.join(", ", missing));
        }
        for (SkillToolSpec tool : skill.tools()) {
            if (registry.registeredNames().contains(tool.exposedName())) {
                throw new SkillException("Skill 专属工具名称冲突: " + tool.exposedName());
            }
        }
    }

    public void activate(LoadedSkill skill) {
        RunState state = requireState();
        if (state.active.containsKey(skill.metadata().name())) return;
        validate(skill);
        List<String> registered = new ArrayList<>();
        try {
            for (SkillToolSpec spec : skill.tools()) {
                Tool tool = toolFactory.apply(spec);
                registry.registerTemporary(tool);
                registered.add(spec.exposedName());
            }
            state.active.put(skill.metadata().name(), skill);
            state.temporaryTools.addAll(registered);
        } catch (RuntimeException exception) {
            registered.forEach(this::removeTool);
            throw exception;
        }
    }

    public List<String> activeSkillNames() {
        Deque<RunState> stack = states.get();
        return stack.isEmpty() ? List.of() : List.copyOf(stack.peek().active.keySet());
    }

    public Optional<SystemReminder> activeReminder() {
        Deque<RunState> stack = states.get();
        if (stack.isEmpty() || stack.peek().active.isEmpty()) return Optional.empty();
        StringBuilder content = new StringBuilder("当前任务已激活以下 Skill。必须持续遵循其 SOP：");
        stack.peek().active.values().forEach(skill -> {
            content.append("\n\n<skill name=\"").append(skill.metadata().name()).append("\">\n")
                    .append(skill.prompt());
            if (!skill.references().isEmpty()) {
                content.append("\n\n参考资料：");
                skill.references().forEach(reference -> content.append("\n\n### ")
                        .append(reference.path()).append('\n').append(reference.content()));
            }
            content.append("\n</skill>");
        });
        return Optional.of(new SystemReminder(ReminderScope.ENVIRONMENT, content.toString()));
    }

    public ToolSelection selectTools(ToolSelection base) {
        Objects.requireNonNull(base, "base");
        Deque<RunState> stack = states.get();
        if (stack.isEmpty() || stack.peek().active.isEmpty()) {
            if (base.unrestricted()) return base;
            LinkedHashSet<String> allowed = new LinkedHashSet<>(base.allowedNames());
            allowed.addAll(SYSTEM_TOOLS);
            return ToolSelection.only(allowed);
        }
        LinkedHashSet<String> skillAllowed = new LinkedHashSet<>();
        stack.peek().active.values().forEach(skill -> skillAllowed.addAll(skill.allowedTools()));
        if (!base.unrestricted()) skillAllowed.retainAll(base.allowedNames());
        skillAllowed.addAll(SYSTEM_TOOLS);
        return ToolSelection.only(skillAllowed);
    }

    public List<ChatMessage> sourceHistory() {
        return List.copyOf(requireState().sourceHistory);
    }

    public AgentEventListener eventListener() { return requireState().listener; }

    private RunState requireState() {
        Deque<RunState> stack = states.get();
        if (stack.isEmpty()) throw new SkillException("当前没有可激活 Skill 的 Agent 任务");
        return stack.peek();
    }

    private void endRun(Deque<RunState> stack, RunState expected) {
        if (stack.isEmpty() || stack.peek() != expected) {
            throw new IllegalStateException("Skill 任务作用域关闭顺序错误");
        }
        stack.pop();
        expected.temporaryTools.forEach(this::removeTool);
        if (stack.isEmpty()) states.remove();
    }

    private void removeTool(String name) {
        registry.unregister(name).ifPresent(tool -> {
            if (tool instanceof AutoCloseable closeable) {
                try { closeable.close(); } catch (Exception ignored) { }
            }
        });
    }

    private static final class RunState {
        private final Map<String, LoadedSkill> active = new LinkedHashMap<>();
        private final List<String> temporaryTools = new ArrayList<>();
        private final List<ChatMessage> sourceHistory;
        private final AgentEventListener listener;

        private RunState(List<ChatMessage> sourceHistory, AgentEventListener listener) {
            this.sourceHistory = List.copyOf(Objects.requireNonNullElse(sourceHistory, List.of()));
            this.listener = listener;
        }
    }
}
