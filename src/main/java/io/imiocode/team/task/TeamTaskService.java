package io.imiocode.team.task;

import io.imiocode.team.TeamException;
import io.imiocode.team.model.TeamConfig;
import io.imiocode.team.model.TeamPrincipal;
import io.imiocode.team.persistence.TeamStore;
import java.util.*;
import java.util.function.BiConsumer;

/** principal 作用域内的任务服务。 */
public final class TeamTaskService {
    private final TeamStore teams;private final TeamTaskStore tasks;private final BiConsumer<String,String> stopMember;
    public TeamTaskService(TeamStore teams,TeamTaskStore tasks,BiConsumer<String,String> stopMember){this.teams=Objects.requireNonNull(teams);this.tasks=Objects.requireNonNull(tasks);this.stopMember=Objects.requireNonNullElse(stopMember,(a,b)->{});}
    public TeamTask create(TeamPrincipal p,String title,String description,String assignee){verify(p);if(assignee!=null&&!assignee.isBlank()&&!teams.require(p.teamName()).members().containsKey(assignee))throw new TeamException("负责人不在团队中");return tasks.create(p.teamName(),title,description,assignee);}
    public TeamTask get(TeamPrincipal p,String id){verify(p);return tasks.require(p.teamName(),id);}
    public List<TeamTask> list(TeamPrincipal p){verify(p);return tasks.list(p.teamName());}
    public TeamTask update(TeamPrincipal p,String id,TeamTaskStatus status,String assignee,String result,String addBlocksOn,String addBlockedBy,Long version){verify(p);if(assignee!=null&&!assignee.isBlank()&&!teams.require(p.teamName()).members().containsKey(assignee))throw new TeamException("负责人不在团队中");return tasks.update(p.teamName(),id,status,assignee,result,addBlocksOn,addBlockedBy,version);}
    public TeamTask stop(TeamPrincipal p,String id){verify(p);TeamTask t=tasks.require(p.teamName(),id);if(t.status().terminal())throw new TeamException("任务已结束");if(!t.assigneeAgentId().isBlank())stopMember.accept(p.teamName(),t.assigneeAgentId());return tasks.update(p.teamName(),id,TeamTaskStatus.STOPPED,null,"任务已停止",t.version());}
    private void verify(TeamPrincipal p){TeamConfig team=teams.require(p.teamName());if(!team.members().containsKey(p.agentId()))throw new TeamException("调用者不在团队中");boolean actualLead=team.leadAgentId().equals(p.agentId());if(actualLead!=p.lead())throw new TeamException("团队角色与花名册身份不匹配");}
}
