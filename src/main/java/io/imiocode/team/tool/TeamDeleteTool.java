package io.imiocode.team.tool;

import com.fasterxml.jackson.databind.node.*;
import io.imiocode.team.runtime.*;
import io.imiocode.tool.*;

/** 经高风险权限确认后停止成员并执行保守的团队资源清理。 */
public final class TeamDeleteTool extends BaseTool {
    private final AgentTeamManager teams;private final TeamToolContext context;
    public TeamDeleteTool(AgentTeamManager teams,TeamToolContext context,ToolLimits limits,SecretRedactor redactor){super(createDefinition(),limits,redactor);this.teams=teams;this.context=context;}
    private static ToolDefinition createDefinition(){ObjectNode s=JsonNodeFactory.instance.objectNode();s.put("type","object");s.putObject("properties").putObject("discard").put("type","boolean");s.put("additionalProperties",false);return new ToolDefinition("TeamDelete","停止成员并安全删除当前团队；有成果的 Worktree 默认保留并拒绝删除。",s,ToolRisk.HIGH);}
    @Override protected ToolResult executeValidated(ObjectNode a){rejectUnknownFields(a,"discard");var p=context.require();TeamDeletionReport r=teams.deleteTeam(p,a.path("discard").asBoolean(false));if(r.deleted()){context.clear(p.teamName());return ToolResult.success("团队已删除: "+p.teamName()+(r.warnings().isEmpty()?"":"；非阻断警告="+r.warnings()));}return ToolResult.failure("团队未删除；保留="+r.retained()+"，警告="+r.warnings());}
}
