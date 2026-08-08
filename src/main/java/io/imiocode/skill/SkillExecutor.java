package io.imiocode.skill;

import io.imiocode.agent.AgentEventListener;
import io.imiocode.agent.AgentEvent;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.tool.ToolResult;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** 显式命令和模型 LoadSkill 共用的执行入口。 */
public final class SkillExecutor {
    private final SkillLoader loader;
    private final SkillActivator activator;
    private final AtomicReference<SkillForkRunner> forkRunner = new AtomicReference<>();

    public SkillExecutor(SkillLoader loader, SkillActivator activator) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.activator = Objects.requireNonNull(activator, "activator");
    }

    public SkillInvocation prepare(String name, String arguments) {
        LoadedSkill skill = loader.load(name, arguments);
        activator.validate(skill);
        return new SkillInvocation(skill, arguments);
    }

    public void setForkRunner(SkillForkRunner runner) {
        if (!forkRunner.compareAndSet(null, Objects.requireNonNull(runner, "runner"))) {
            throw new IllegalStateException("Skill fork runner 已设置");
        }
    }

    public String executeFork(SkillInvocation invocation, List<ChatMessage> history,
                              AgentEventListener listener) {
        SkillForkRunner runner = forkRunner.get();
        if (runner == null) throw new SkillException("fork Skill 执行器尚未初始化");
        return runner.run(invocation, history,
                Objects.requireNonNullElse(listener, AgentEventListener.NOOP));
    }

    public ToolResult executeFromTool(String name, String arguments) {
        try {
            SkillInvocation invocation = prepare(name, arguments);
            if (invocation.skill().metadata().mode() == SkillMode.INLINE) {
                activator.activate(invocation.skill());
                return ToolResult.success("Skill 已激活: " + invocation.skill().metadata().name()
                        + "。完整 SOP 将在下一轮上下文中生效。");
            }
            AgentEventListener parent = activator.eventListener();
            AgentEventListener nestedEvents = event -> {
                if (event instanceof AgentEvent.PermissionRequested
                        || event instanceof AgentEvent.PermissionResolved
                        || event instanceof AgentEvent.ToolExecutionChanged
                        || event instanceof AgentEvent.ContextChanged) {
                    parent.onEvent(event);
                }
            };
            String result = executeFork(invocation, activator.sourceHistory(), nestedEvents);
            return ToolResult.success("fork Skill " + invocation.skill().metadata().name()
                    + " 已完成：\n" + result);
        } catch (SkillException exception) {
            return ToolResult.failure(exception.getMessage());
        } catch (RuntimeException exception) {
            return ToolResult.failure("Skill 执行失败");
        }
    }

    public SkillLoader loader() { return loader; }

    public void cancelFork() {
        SkillForkRunner runner = forkRunner.get();
        if (runner != null) runner.cancelActive();
    }

    public boolean isForkActive() {
        SkillForkRunner runner = forkRunner.get();
        return runner != null && runner.isActive();
    }
}
