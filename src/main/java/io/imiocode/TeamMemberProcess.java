package io.imiocode;

import io.imiocode.agent.AgentEventListener;
import io.imiocode.agent.AgentRequest;
import io.imiocode.config.ConfigLoader;
import io.imiocode.config.RuntimeConfig;
import io.imiocode.conversation.ChatMessage;
import io.imiocode.conversation.MessageRole;
import io.imiocode.conversation.SystemReminder;
import io.imiocode.hook.HookRuntime;
import io.imiocode.hook.integration.HookContextFactory;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.subagent.definition.AgentDefinitionLoader;
import io.imiocode.subagent.definition.AgentIsolation;
import io.imiocode.subagent.filter.SubagentToolFilter;
import io.imiocode.subagent.model.ModelAliasResolver;
import io.imiocode.subagent.runtime.SubagentAgentHandle;
import io.imiocode.subagent.runtime.SubagentToolRegistryFactory;
import io.imiocode.team.TeamException;
import io.imiocode.team.mailbox.MailboxStore;
import io.imiocode.team.backend.JdkProcessExecutor;
import io.imiocode.team.backend.ProcessResult;
import io.imiocode.team.model.TeamConfig;
import io.imiocode.team.model.TeamPrincipal;
import io.imiocode.team.model.TeamRole;
import io.imiocode.team.model.TeammateInfo;
import io.imiocode.team.model.TeammateStatus;
import io.imiocode.team.persistence.TeamPaths;
import io.imiocode.team.persistence.TeamStore;
import io.imiocode.team.persistence.TranscriptEntry;
import io.imiocode.team.persistence.TranscriptRole;
import io.imiocode.team.persistence.TranscriptStore;
import io.imiocode.team.runtime.TeamMemberRunner;
import io.imiocode.team.runtime.TeamMemberWorker;
import io.imiocode.team.runtime.TeamMessenger;
import io.imiocode.team.runtime.SendReceipt;
import io.imiocode.team.task.TeamTaskService;
import io.imiocode.team.task.TeamTaskStore;
import io.imiocode.team.tool.SendMessageTool;
import io.imiocode.team.tool.TaskCreateTool;
import io.imiocode.team.tool.TaskGetTool;
import io.imiocode.team.tool.TaskListTool;
import io.imiocode.team.tool.TaskStopTool;
import io.imiocode.team.tool.TaskUpdateTool;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.Tool;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolSelection;
import io.imiocode.tool.core.BashTool;
import io.imiocode.tool.core.EditFileTool;
import io.imiocode.tool.core.GlobTool;
import io.imiocode.tool.core.GrepTool;
import io.imiocode.tool.core.ReadFileTool;
import io.imiocode.tool.core.WriteFileTool;
import io.imiocode.tool.workspace.WorkspacePolicy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.time.Duration;

/** `--team-member` 的受限非交互入口；命令行只携带身份和路径，不携带 Prompt 或密钥。 */
final class TeamMemberProcess {
    private TeamMemberProcess() { }

    static boolean requested(String[] args) {
        return args != null && args.length > 0 && "--team-member".equals(args[0]);
    }

    static int run(String[] args) {
        try {
            Arguments launch = Arguments.parse(args);
            Path repository = launch.repository().toAbsolutePath().normalize();
            Path workspace = launch.workspace().toAbsolutePath().normalize();
            if (!Files.isDirectory(repository) || !Files.isDirectory(workspace)) {
                throw new TeamException("成员仓库或 Worktree 不存在");
            }
            Path userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
            RuntimeConfig runtime = new ConfigLoader().loadAll(repository, userHome, System.getenv());
            ToolLimits limits = ToolLimits.defaults();
            SecretRedactor redactor = runtime.redactor();
            TeamPaths paths = new TeamPaths(repository);
            TeamStore teams = new TeamStore(paths);
            TeamConfig team = teams.require(launch.team());
            TeammateInfo member = team.members().get(launch.agent());
            if (member == null || member.agentId().equals(team.leadAgentId())) {
                throw new TeamException("成员身份不在花名册中");
            }
            if (!member.worktree().toAbsolutePath().normalize().equals(workspace)) {
                throw new TeamException("成员 Worktree 与花名册不一致");
            }

            MailboxStore mailbox = new MailboxStore(paths, redactor,
                    runtime.teams().maxMessageChars(), runtime.teams().maxMessagesPerMailbox(),
                    runtime.teams().maxTranscriptBytes());
            TranscriptStore transcripts = new TranscriptStore(paths, redactor,
                    runtime.teams().maxMessageChars(), runtime.teams().maxTranscriptBytes());
            TeamTaskStore taskStore = new TeamTaskStore(paths, runtime.teams().maxTasksPerTeam());
            TeamTaskService taskService = new TeamTaskService(teams, taskStore, (ignoredTeam, ignoredAgent) -> { });
            TeamPrincipal principal = new TeamPrincipal(team.name(), member.agentId(), TeamRole.MEMBER);
            java.util.function.Supplier<TeamPrincipal> identity = () -> principal;

            List<Tool> teamTools = new ArrayList<>();
            teamTools.add(new TaskCreateTool(taskService, identity, limits, redactor));
            teamTools.add(new TaskGetTool(taskService, identity, limits, redactor));
            teamTools.add(new TaskListTool(taskService, identity, limits, redactor));
            teamTools.add(new TaskUpdateTool(taskService, identity, limits, redactor));
            teamTools.add(new TaskStopTool(taskService, identity, limits, redactor));
            TeamMessenger messenger = (sender, recipient, type, summary, body, taskId) -> {
                TeamConfig current = teams.require(sender.teamName());
                if (!current.members().containsKey(sender.agentId())) {
                    throw new TeamException("调用者不在团队中");
                }
                if (type == io.imiocode.team.mailbox.MailboxMessageType.PLAN_APPROVAL) {
                    throw new TeamException("只有 Lead 可以审批计划");
                }
                List<io.imiocode.team.mailbox.MailboxMessage> sent;
                if ("*".equals(recipient)) {
                    sent = mailbox.broadcast(current.name(), sender.agentId(),
                            current.members().keySet(), type, summary, body, taskId);
                } else {
                    if (!current.members().containsKey(recipient)) {
                        throw new TeamException("收件人不在团队中");
                    }
                    sent = List.of(mailbox.send(current.name(), sender.agentId(), recipient,
                            type, summary, body, taskId));
                }
                List<String> warnings = new ArrayList<>();
                for (var delivered : sent) {
                    wakeExternalTmux(repository, current, delivered.recipientAgentId(), warnings);
                }
                return new SendReceipt(sent.stream().map(io.imiocode.team.mailbox.MailboxMessage::id).toList(),
                        warnings, List.of());
            };
            teamTools.add(new SendMessageTool(messenger, identity, limits, redactor));

            ToolRegistry rootTools = coreTools(repository, limits, redactor);
            SubagentToolRegistryFactory scopedTools = new SubagentToolRegistryFactory(
                    rootTools, limits, redactor);
            AgentDefinitionLoader definitions = new AgentDefinitionLoader(repository, userHome);
            AgentDefinition definition = definitions.snapshot().find(member.agentType())
                    .orElseThrow(() -> new TeamException("未知团队 Agent 类型: " + member.agentType()))
                    .withIsolation(AgentIsolation.NONE);
            SubagentToolFilter filter = new SubagentToolFilter(runtime.subagents());
            ModelAliasResolver aliases = new ModelAliasResolver(runtime.subagents());
            ToolSelection selection = filter.select(definition, rootTools.enabledNames(), false);
            LinkedHashSet<String> allowed = new LinkedHashSet<>(selection.allowedNames());
            teamTools.forEach(tool -> allowed.add(tool.definition().name()));
            ToolSelection memberSelection = ToolSelection.only(allowed);
            HookContextFactory hooks = new HookContextFactory(workspace);

            TeamMemberRunner runner = (teamName, agentId, agentType, worktree, prompt, history) -> {
                List<ChatMessage> messages = history.stream()
                        .filter(entry -> entry.role() == TranscriptRole.USER
                                || entry.role() == TranscriptRole.ASSISTANT)
                        .map(TeamMemberProcess::chatMessage)
                        .toList();
                String memberOutput;
                try (SubagentAgentHandle handle = ImioCodeApplication.createSubagentHandle(
                        definition, memberSelection, worktree, scopedTools, runtime.app(), aliases,
                        runtime.permissions(), redactor, HookRuntime.NOOP, hooks, teamTools, true)) {
                    String roster = teams.require(teamName).members().values().stream()
                            .map(item -> item.agentId() + "(" + item.agentType() + ")")
                            .collect(java.util.stream.Collectors.joining(", "));
                    var result = handle.agent().run(new AgentRequest(
                            messages,
                            new ChatMessage(MessageRole.USER, prompt),
                            List.of(new SystemReminder("你是团队 " + teamName + " 的成员 " + agentId
                                    + "。花名册：" + roster
                                    + "。通过 Task 工具和 SendMessage 协作；工作区是你的独立 Worktree。")),
                            memberSelection), event -> persistToolEvent(
                                    transcripts, teamName, agentId, event,
                                    runtime.teams().maxMessageChars()));
                    if (!result.completed()) {
                        throw new TeamException("团队成员未正常完成: " + result.stopReason());
                    }
                    memberOutput = result.finalResponse().orElseThrow().text();
                }
                // 独立 Agent 的执行器关闭信号不得结束外层 Mailbox 工作循环。
                Thread.interrupted();
                return memberOutput;
            };
            new TeamMemberWorker(teams, mailbox, transcripts, runner)
                    .run(team.name(), member.agentId(), member.agentType(), workspace);
            return teams.require(team.name()).members().get(member.agentId()).status()
                    == TeammateStatus.FAILED ? 1 : 0;
        } catch (RuntimeException exception) {
            System.err.println("[Team成员错误] " + safeMessage(exception));
            return 2;
        }
    }

    private static ToolRegistry coreTools(Path workspace, ToolLimits limits, SecretRedactor redactor) {
        WorkspacePolicy policy = new WorkspacePolicy(workspace);
        ToolRegistry registry = new ToolRegistry();
        registry.register(new ReadFileTool(policy, limits, redactor));
        registry.register(new WriteFileTool(policy, limits, redactor));
        registry.register(new EditFileTool(policy, limits, redactor));
        registry.register(new BashTool(policy, limits, redactor));
        registry.register(new GlobTool(policy, limits, redactor));
        registry.register(new GrepTool(policy, limits, redactor));
        return registry;
    }

    /** 外部成员无法持有 Lead 进程的后端对象，因此按花名册验证 pane 身份后发送固定唤醒词。 */
    private static void wakeExternalTmux(Path repository, TeamConfig team, String recipient,
                                         List<String> warnings) {
        TeammateInfo target = team.members().get(recipient);
        if (target == null || target.backend() != io.imiocode.team.model.TeamBackend.TMUX) return;
        String pane = target.backendHandle();
        if (!pane.matches("%[0-9]+")) {
            warnings.add("成员 " + recipient + " 的 tmux 句柄无效，消息已持久化");
            return;
        }
        JdkProcessExecutor processes = new JdkProcessExecutor();
        ProcessResult title = processes.run(repository,
                List.of("tmux", "display-message", "-p", "-t", pane, "#{pane_title}"),
                Duration.ofSeconds(2));
        String expected = "imiocode-" + team.name() + "-" + recipient;
        if (!title.success() || !expected.equals(title.output().trim())) {
            warnings.add("成员 " + recipient + " 的 tmux pane 归属验证失败，消息已持久化");
            return;
        }
        ProcessResult wake = processes.run(repository,
                List.of("tmux", "send-keys", "-t", pane, "IMIO_MAILBOX_WAKE", "Enter"),
                Duration.ofSeconds(2));
        if (!wake.success()) warnings.add("成员 " + recipient + " 的 tmux 唤醒失败，消息已持久化");
    }

    private static ChatMessage chatMessage(TranscriptEntry entry) {
        return new ChatMessage(entry.role() == TranscriptRole.USER
                ? MessageRole.USER : MessageRole.ASSISTANT, entry.content());
    }

    private static void persistToolEvent(TranscriptStore transcripts, String team, String agent,
                                         io.imiocode.agent.AgentEvent event, int maxChars) {
        if (!(event instanceof io.imiocode.agent.AgentEvent.ToolExecutionChanged changed)
                || (changed.execution().state() != io.imiocode.tool.ToolExecutionState.SUCCEEDED
                && changed.execution().state() != io.imiocode.tool.ToolExecutionState.FAILED)) return;
        String detail = changed.execution().result() == null ? ""
                : changed.execution().result().success()
                ? changed.execution().result().output() : changed.execution().result().error();
        String content = changed.execution().call().name() + " " + changed.execution().state()
                + (detail == null || detail.isBlank() ? "" : ": " + detail);
        if (content.length() > maxChars) content = content.substring(0, maxChars);
        transcripts.append(team, agent, TranscriptRole.TOOL, content,
                changed.execution().call().id());
    }

    private static String safeMessage(RuntimeException exception) {
        return exception instanceof TeamException ? "成员身份、持久状态或运行边界验证失败"
                : "成员进程无法继续运行";
    }

    private record Arguments(String team, String agent, Path repository, Path workspace) {
        static Arguments parse(String[] args) {
            if (args == null || args.length != 7 || !"--team-member".equals(args[0])
                    || !"--repository".equals(args[3]) || !"--workspace".equals(args[5])) {
                throw new TeamException("成员入口参数无效");
            }
            TeamPaths validator = new TeamPaths(Path.of(args[4]));
            return new Arguments(validator.requireSlug(args[1], "team_name"),
                    validator.requireSlug(args[2], "agent_id"), Path.of(args[4]), Path.of(args[6]));
        }
    }
}
