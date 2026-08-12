package io.imiocode.subagent.runtime;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;

import java.util.Objects;
import io.imiocode.team.model.TeamBackend;
import io.imiocode.team.runtime.AgentTeamManager;
import io.imiocode.team.runtime.TeammateSpawnRequest;
import io.imiocode.team.tool.TeamToolContext;

/** 把全部子 Agent 定义暴露为一个稳定的 agent 工具。 */
public final class AgentTool extends BaseTool {
    private final SubagentDispatcher dispatcher;
    private final AgentTeamManager teams;
    private final TeamToolContext teamContext;
    public AgentTool(SubagentDispatcher dispatcher, ToolLimits limits, SecretRedactor redactor) {
        this(dispatcher, null, null, limits, redactor);
    }
    public AgentTool(SubagentDispatcher dispatcher, AgentTeamManager teams, TeamToolContext teamContext,
                     ToolLimits limits, SecretRedactor redactor) {
        super(createDefinition(Objects.requireNonNull(dispatcher)), limits, redactor);
        this.dispatcher=dispatcher; this.teams=teams; this.teamContext=teamContext;
    }
    private static ToolDefinition createDefinition(SubagentDispatcher dispatcher) {
        ObjectNode schema= JsonNodeFactory.instance.objectNode(); schema.put("type","object");
        ObjectNode properties=schema.putObject("properties");
        properties.putObject("subagent_type").put("type","string").put("description",
                "可选 Agent 定义名称；缺省时从完整父历史 Fork。可用定义：" + dispatcher.catalogSummary());
        properties.putObject("description").put("type","string").put("description","供任务列表显示的简短描述");
        properties.putObject("prompt").put("type","string").put("description","交给子 Agent 的完整任务 Prompt");
        properties.putObject("run_in_background").put("type","boolean").put("description","是否后台执行");
        ObjectNode isolation = properties.putObject("isolation");
        isolation.put("type", "string");
        isolation.putArray("enum").add("none").add("worktree");
        isolation.put("description", "可选隔离模式；省略时继承 Agent 定义");
        properties.putObject("model").put("type","string").put("description","可选模型名或 config.yaml 中的逻辑别名，优先级高于 Agent 定义");
        properties.putObject("cwd").put("type","string").put("description","可选工作子目录，必须位于当前工作区内");
        properties.putObject("team_name").put("type","string").put("description","可选团队名；提供后生成持久具名队员");
        properties.putObject("name").put("type","string").put("description","团队内可选成员名");
        properties.putObject("backend").put("type","string").putArray("enum").add("auto").add("tmux").add("iterm2").add("in-process");
        properties.putObject("plan_approval_required").put("type","boolean");
        ObjectNode denied=properties.putObject("disallowed_tools"); denied.put("type","array"); denied.putObject("items").put("type","string");
        var required=schema.putArray("required"); required.add("description"); required.add("prompt");
        schema.put("additionalProperties",false);
        return new ToolDefinition("agent",
                "委派独立任务给专用子 Agent。探索代码优先用 explore，制定只读方案用 plan，复杂实现用 general-purpose；互不依赖的长任务可后台运行。",
                schema, ToolRisk.LOW);
    }
    @Override protected ToolResult executeValidated(ObjectNode arguments) throws Exception {
        rejectUnknownFields(arguments,"subagent_type","description","prompt","run_in_background","isolation","model","cwd","disallowed_tools","team_name","name","backend","plan_approval_required");
        String type=arguments.path("subagent_type").asText(""); String description=requireText(arguments,"description");
        String prompt=requireText(arguments,"prompt");
        boolean background=arguments.path("run_in_background").asBoolean(false);
        String isolation=arguments.path("isolation").asText("");
        String model=arguments.path("model").asText("");
        String cwd=arguments.path("cwd").asText(".");
        String teamName=arguments.path("team_name").asText("");
        if (!teamName.isBlank()) {
            if (teams == null || teamContext == null) throw new IllegalArgumentException("团队功能尚未初始化");
            var principal = teamContext.require();
            if (!principal.teamName().equals(teamName)) throw new IllegalArgumentException("team_name 不是当前团队");
            var member = teams.spawn(principal, new TeammateSpawnRequest(
                    arguments.path("name").asText(""), type, model,
                    TeamBackend.parse(arguments.path("backend").asText("auto")),
                    arguments.has("plan_approval_required") ? arguments.path("plan_approval_required").asBoolean() : null,
                    prompt));
            return ToolResult.success("团队成员已启动: agent_id=" + member.agentId()
                    + "，backend=" + member.backend().configValue()
                    + "，worktree=" + member.worktree());
        }
        java.util.LinkedHashSet<String> deniedTools=new java.util.LinkedHashSet<>();
        var deniedNode=arguments.path("disallowed_tools");
        if (!deniedNode.isMissingNode()) {
            if (!deniedNode.isArray()) throw new IllegalArgumentException("disallowed_tools 必须是字符串数组");
            for (var item:deniedNode) {
                if (!item.isTextual() || item.textValue().isBlank()) throw new IllegalArgumentException("disallowed_tools 必须是字符串数组");
                deniedTools.add(item.textValue().trim());
            }
        }
        SubagentDispatchResult result=dispatcher.dispatch(type,description,prompt,background,isolation,model,cwd,deniedTools);
        return result.success()?ToolResult.success(result.message()):ToolResult.failure(result.message());
    }

    @Override public void cancel() { dispatcher.cancelActiveSynchronous(); }
}
