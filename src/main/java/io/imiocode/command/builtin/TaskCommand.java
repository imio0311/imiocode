package io.imiocode.command.builtin;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandMessage;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandType;
import io.imiocode.subagent.task.TaskManager;
import io.imiocode.subagent.task.TaskSnapshot;

import java.util.List;
import java.util.Set;

/** /tasks、/task info、/task cancel 的本地入口。 */
public final class TaskCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "task", Set.of("tasks"), "/tasks | /task info <id> | /task cancel <id>",
            "查看或取消后台子 Agent", CommandType.LOCAL);
    private final TaskManager tasks;
    public TaskCommand(TaskManager tasks) { this.tasks=tasks; }
    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }
    @Override public CommandResult execute(CommandContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            List<TaskSnapshot> list=tasks.list();
            if (list.isEmpty()) return CommandResult.handled(CommandMessage.info("[任务] 暂无后台任务"));
            StringBuilder text=new StringBuilder("[任务]\n");
            for (TaskSnapshot task:list) text.append(task.id()).append(" · ").append(task.agentName())
                    .append(" · ").append(task.status().name().toLowerCase()).append(" · ")
                    .append(task.description(),0,Math.min(80,task.description().length())).append('\n');
            return CommandResult.handled(CommandMessage.info(text.toString().stripTrailing()));
        }
        if (arguments.size()==2 && "info".equalsIgnoreCase(arguments.get(0))) {
            TaskSnapshot task=tasks.find(arguments.get(1)).orElseThrow(()->new IllegalArgumentException("未知任务: "+arguments.get(1)));
            return CommandResult.handled(CommandMessage.info("[任务/"+task.id()+"]\nAgent: "+task.agentName()
                    +"\nStatus: "+task.status().name().toLowerCase()+"\nDescription: "+task.description()
                    +task.traceId().map(v->"\nTrace: "+v).orElse("")
                    +task.output().map(v->"\nOutput: "+v).orElse("")));
        }
        if (arguments.size()==2 && "cancel".equalsIgnoreCase(arguments.get(0))) {
            boolean cancelled=tasks.cancel(arguments.get(1));
            return CommandResult.handled(CommandMessage.info(cancelled?"[任务] 已取消 "+arguments.get(1):"[任务] 不存在或已结束"));
        }
        throw new IllegalArgumentException(DESCRIPTOR.usage());
    }
}
