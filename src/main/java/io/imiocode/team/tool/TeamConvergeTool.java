package io.imiocode.team.tool;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.team.runtime.AgentTeamManager;
import io.imiocode.team.runtime.ConvergenceReport;
import io.imiocode.tool.BaseTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolDefinition;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolResult;
import io.imiocode.tool.ToolRisk;

import java.time.Duration;
import java.util.Objects;

/** Lead 可调用的团队收敛快照；只观察和等待，不自动合并或丢弃成果。 */
public final class TeamConvergeTool extends BaseTool {
    private final AgentTeamManager teams;
    private final TeamToolContext context;

    public TeamConvergeTool(AgentTeamManager teams, TeamToolContext context,
                            ToolLimits limits, SecretRedactor redactor) {
        super(createDefinition(), limits, redactor);
        this.teams = Objects.requireNonNull(teams);
        this.context = Objects.requireNonNull(context);
    }

    private static ToolDefinition createDefinition() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode wait = schema.putObject("properties").putObject("wait_millis");
        wait.put("type", "integer");
        wait.put("minimum", 0);
        wait.put("maximum", 30_000);
        schema.put("additionalProperties", false);
        return new ToolDefinition("TeamConverge",
                "等待团队达到可收敛状态并汇总成员结果、任务、未读消息和 Worktree 安全状态；不会自动合并或丢弃。",
                schema, ToolRisk.LOW);
    }

    @Override
    protected ToolResult executeValidated(ObjectNode arguments) {
        rejectUnknownFields(arguments, "wait_millis");
        long waitMillis = arguments.path("wait_millis").asLong(0);
        if (waitMillis < 0 || waitMillis > 30_000) {
            throw new IllegalArgumentException("wait_millis 必须在 0..30000 之间");
        }
        ConvergenceReport report = teams.converge(context.require(), Duration.ofMillis(waitMillis));
        ObjectNode output = JsonNodeFactory.instance.objectNode();
        output.put("team", report.teamName());
        output.put("converged", report.converged());
        var members = output.putArray("members");
        report.memberStates().forEach(members::add);
        var results = output.putArray("results");
        report.memberResults().forEach(results::add);
        var tasks = output.putArray("tasks");
        report.taskStates().forEach(tasks::add);
        var unread = output.putArray("unreadMail");
        report.unreadMail().forEach(unread::add);
        var worktrees = output.putArray("worktrees");
        report.worktrees().forEach(worktrees::add);
        return ToolResult.success(output.toPrettyString());
    }
}
