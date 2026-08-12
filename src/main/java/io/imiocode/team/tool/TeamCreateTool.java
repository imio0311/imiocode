package io.imiocode.team.tool;

import com.fasterxml.jackson.databind.node.*;
import io.imiocode.team.model.*;
import io.imiocode.team.runtime.*;
import io.imiocode.tool.*;
import java.util.Objects;

/** 创建持久团队并将当前 Agent 绑定为该团队 Lead。 */
public final class TeamCreateTool extends BaseTool {
    private final AgentTeamManager teams;private final TeamToolContext context;
    public TeamCreateTool(AgentTeamManager teams,TeamToolContext context,ToolLimits limits,SecretRedactor redactor){super(createDefinition(),limits,redactor);this.teams=Objects.requireNonNull(teams);this.context=Objects.requireNonNull(context);}
    private static ToolDefinition createDefinition(){ObjectNode s=JsonNodeFactory.instance.objectNode();s.put("type","object");ObjectNode p=s.putObject("properties");p.putObject("team_name").put("type","string");p.putObject("description").put("type","string");p.putObject("agent_type").put("type","string");p.putObject("backend").put("type","string").putArray("enum").add("auto").add("tmux").add("iterm2").add("in-process");s.putArray("required").add("team_name");s.put("additionalProperties",false);return new ToolDefinition("TeamCreate","创建本仓库的持久 Agent Team，并把当前 Agent 注册为 Lead。",s,ToolRisk.LOW);}
    @Override protected ToolResult executeValidated(ObjectNode a){rejectUnknownFields(a,"team_name","description","agent_type","backend");String name=requireText(a,"team_name");TeamConfig t=teams.createTeam(new TeamCreateRequest(name,a.path("description").asText(""),"lead","Lead",a.path("agent_type").asText("lead"),TeamBackend.parse(a.path("backend").asText("auto"))));context.select(new TeamPrincipal(t.name(),t.leadAgentId(),TeamRole.LEAD));return ToolResult.success("团队已创建: "+t.name()+"，lead="+t.leadAgentId());}
}
