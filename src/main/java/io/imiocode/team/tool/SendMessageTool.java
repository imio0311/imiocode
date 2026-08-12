package io.imiocode.team.tool;

import com.fasterxml.jackson.databind.node.*;
import io.imiocode.team.mailbox.MailboxMessageType;
import io.imiocode.team.model.TeamPrincipal;
import io.imiocode.team.runtime.*;
import io.imiocode.tool.*;
import java.util.function.Supplier;

/** 以当前团队身份发送持久 Mailbox 消息，并按需恢复 idle 成员。 */
public final class SendMessageTool extends BaseTool {
    private final TeamMessenger teams;private final Supplier<TeamPrincipal> principal;
    public SendMessageTool(TeamMessenger teams,Supplier<TeamPrincipal> principal,ToolLimits limits,SecretRedactor redactor){super(createDefinition(),limits,redactor);this.teams=teams;this.principal=principal;}
    private static ToolDefinition createDefinition(){ObjectNode s=JsonNodeFactory.instance.objectNode();s.put("type","object");ObjectNode p=s.putObject("properties");p.putObject("recipient").put("type","string").put("description","成员 agent ID，或 * 广播");p.putObject("summary").put("type","string").put("description","简短预览；PLAN_APPROVAL 时必须为 APPROVED 或 REJECTED");p.putObject("body").put("type","string").put("description","消息正文；REJECTED 计划审批必须包含反馈");p.putObject("type").put("type","string").putArray("enum").add("MESSAGE").add("TASK").add("STOP_REQUEST").add("STOP_RESPONSE").add("PLAN_APPROVAL");p.putObject("task_id").put("type","string");s.putArray("required").add("recipient").add("body");s.put("additionalProperties",false);return new ToolDefinition("SendMessage","向本团队单个成员或所有成员发送持久消息；idle 成员会以原身份续写。",s,ToolRisk.LOW);}
    @Override protected ToolResult executeValidated(ObjectNode a){rejectUnknownFields(a,"recipient","summary","body","type","task_id");SendReceipt r=teams.send(principal.get(),requireText(a,"recipient"),MailboxMessageType.valueOf(a.path("type").asText("MESSAGE")),a.path("summary").asText(""),requireText(a,"body"),a.path("task_id").asText(""));return ToolResult.success("消息已持久化: "+r.messageIds()+"；恢复成员="+r.resumedAgentIds()+"；警告="+r.warnings());}
}
