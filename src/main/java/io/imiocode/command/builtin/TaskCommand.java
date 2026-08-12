package io.imiocode.command.builtin;

import io.imiocode.command.Command;
import io.imiocode.command.CommandContext;
import io.imiocode.command.CommandDescriptor;
import io.imiocode.command.CommandMessage;
import io.imiocode.command.CommandResult;
import io.imiocode.command.CommandType;
import io.imiocode.subagent.task.TaskManager;
import io.imiocode.subagent.task.TaskSnapshot;
import io.imiocode.team.model.TeamPrincipal;
import io.imiocode.team.task.TeamTask;
import io.imiocode.team.task.TeamTaskService;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.function.Supplier;

/** /tasks、/task info、/task cancel 的本地入口。 */
public final class TaskCommand implements Command {
    private static final CommandDescriptor DESCRIPTOR = new CommandDescriptor(
            "task", Set.of("tasks"), "/tasks | /task info <id> | /task cancel <id>",
            "查看或取消后台子 Agent", CommandType.LOCAL);
    private final TaskManager tasks;
    private final TeamTaskService teamTasks;
    private final Supplier<Optional<TeamPrincipal>> teamPrincipal;
    public TaskCommand(TaskManager tasks) { this(tasks,null,Optional::empty); }
    public TaskCommand(TaskManager tasks,TeamTaskService teamTasks,
                       Supplier<Optional<TeamPrincipal>> teamPrincipal) {
        this.tasks=tasks;this.teamTasks=teamTasks;this.teamPrincipal=teamPrincipal==null?Optional::empty:teamPrincipal;
    }
    @Override public CommandDescriptor descriptor() { return DESCRIPTOR; }
    @Override public CommandResult execute(CommandContext context, List<String> arguments) {
        if (arguments.isEmpty()) {
            List<TaskSnapshot> list=tasks.list();
            List<TeamTask> teamList=currentTeamTasks();
            if (list.isEmpty()&&teamList.isEmpty()) return CommandResult.handled(CommandMessage.info("[任务] 暂无后台或团队任务"));
            StringBuilder text=new StringBuilder("[任务]\n");
            for (TaskSnapshot task:list) text.append(task.id()).append(" · ").append(task.agentName())
                    .append(" · ").append(task.status().name().toLowerCase()).append(" · ")
                    .append(task.description(),0,Math.min(80,task.description().length())).append('\n');
            for(TeamTask task:teamList)text.append(task.id()).append(" · team/")
                    .append(task.assigneeAgentId().isBlank()?"unassigned":task.assigneeAgentId())
                    .append(" · ").append(task.status().name().toLowerCase()).append(" · ")
                    .append(task.title(),0,Math.min(80,task.title().length())).append('\n');
            return CommandResult.handled(CommandMessage.info(text.toString().stripTrailing()));
        }
        if (arguments.size()==2 && "info".equalsIgnoreCase(arguments.get(0))) {
            Optional<TaskSnapshot> old=tasks.find(arguments.get(1));
            if(old.isPresent()){TaskSnapshot task=old.get();return CommandResult.handled(CommandMessage.info("[任务/"+task.id()+"]\nAgent: "+task.agentName()
                    +"\nStatus: "+task.status().name().toLowerCase()+"\nDescription: "+task.description()
                    +task.traceId().map(v->"\nTrace: "+v).orElse("")
                    +task.output().map(v->"\nOutput: "+v).orElse("")));}
            TeamTask task=requireTeamTask(arguments.get(1));
            return CommandResult.handled(CommandMessage.info("[团队任务/"+task.id()+"]\nAssignee: "+
                    (task.assigneeAgentId().isBlank()?"unassigned":task.assigneeAgentId())+"\nStatus: "+
                    task.status().name().toLowerCase()+"\nTitle: "+task.title()+"\nDescription: "+task.description()+
                    (task.result().isBlank()?"":"\nResult: "+task.result())));
        }
        if (arguments.size()==2 && "cancel".equalsIgnoreCase(arguments.get(0))) {
            boolean cancelled;
            if(tasks.find(arguments.get(1)).isPresent())cancelled=tasks.cancel(arguments.get(1));
            else{TeamTask task=requireTeamTask(arguments.get(1));teamTasks.stop(teamPrincipal.get().orElseThrow(),task.id());cancelled=true;}
            return CommandResult.handled(CommandMessage.info(cancelled?"[任务] 已取消 "+arguments.get(1):"[任务] 不存在或已结束"));
        }
        throw new IllegalArgumentException(DESCRIPTOR.usage());
    }
    private List<TeamTask> currentTeamTasks(){if(teamTasks==null)return List.of();return teamPrincipal.get().map(teamTasks::list).orElseGet(List::of);}
    private TeamTask requireTeamTask(String id){if(teamTasks==null)throw new IllegalArgumentException("未知任务: "+id);TeamPrincipal p=teamPrincipal.get().orElseThrow(()->new IllegalArgumentException("未知任务: "+id));return teamTasks.get(p,id);}
}
