package io.imiocode;

import io.imiocode.agent.Agent;
import io.imiocode.agent.AgentHistoryContext;
import io.imiocode.agent.AgentEvent;
import io.imiocode.command.CommandRegistry;
import io.imiocode.command.builtin.ClearCommand;
import io.imiocode.command.builtin.CompactCommand;
import io.imiocode.command.builtin.DoCommand;
import io.imiocode.command.builtin.ExitCommand;
import io.imiocode.command.builtin.HelpCommand;
import io.imiocode.command.builtin.MemoryCommand;
import io.imiocode.command.builtin.PlanCommand;
import io.imiocode.command.builtin.PermissionCommand;
import io.imiocode.command.builtin.ReviewCommand;
import io.imiocode.command.builtin.SessionCommand;
import io.imiocode.command.builtin.StatusCommand;
import io.imiocode.command.builtin.VerbosityCommand;
import io.imiocode.command.builtin.TaskCommand;
import io.imiocode.command.builtin.WorktreeCommand;
import io.imiocode.config.AppConfig;
import io.imiocode.config.ConfigException;
import io.imiocode.config.ConfigLoader;
import io.imiocode.config.ConfigNotice;
import io.imiocode.config.RuntimeConfig;
import io.imiocode.config.UiVerbosity;
import io.imiocode.context.ApproximateTokenEstimator;
import io.imiocode.context.ContextManager;
import io.imiocode.context.ConversationSerializer;
import io.imiocode.context.ConversationSummarizer;
import io.imiocode.context.SummaryParser;
import io.imiocode.context.ToolResultOffloader;
import io.imiocode.context.ToolResultSpillStore;
import io.imiocode.hook.DefaultHookEngine;
import io.imiocode.hook.HookEvent;
import io.imiocode.hook.HookRuntime;
import io.imiocode.hook.action.ActionDispatcher;
import io.imiocode.hook.action.AgentPlaceholderHookExecutor;
import io.imiocode.hook.action.CommandHookExecutor;
import io.imiocode.hook.action.HttpHookExecutor;
import io.imiocode.hook.action.JdkHookHttpTransport;
import io.imiocode.hook.action.JdkHookProcessRunner;
import io.imiocode.hook.action.PromptHookExecutor;
import io.imiocode.hook.condition.DefaultConditionEvaluator;
import io.imiocode.hook.integration.HookContextFactory;
import io.imiocode.hook.integration.HookToolLifecycleListener;
import io.imiocode.hook.template.HookTemplateResolver;
import io.imiocode.conversation.ConversationSession;
import io.imiocode.instruction.FileInstructionLoader;
import io.imiocode.instruction.InstructionLoadRequest;
import io.imiocode.instruction.InstructionReminderFormatter;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmClientFactory;
import io.imiocode.memory.LlmMemoryExtractor;
import io.imiocode.memory.MarkdownMemoryStore;
import io.imiocode.memory.MemoryManager;
import io.imiocode.memory.MemoryReminderFormatter;
import io.imiocode.memory.MemoryResponseParser;
import io.imiocode.memory.MemorySafetyPolicy;
import io.imiocode.memory.MemoryScope;
import io.imiocode.mcp.config.McpConfigError;
import io.imiocode.mcp.config.McpConfigLoadResult;
import io.imiocode.mcp.jsonrpc.JsonRpcCodec;
import io.imiocode.mcp.manager.McpManager;
import io.imiocode.mcp.manager.McpStartupResult;
import io.imiocode.mcp.transport.McpTransportFactory;
import io.imiocode.prompt.EnvironmentContextCollector;
import io.imiocode.prompt.EnvironmentReminderFormatter;
import io.imiocode.prompt.PromptAssembler;
import io.imiocode.prompt.SystemPromptBuilder;
import io.imiocode.persistence.DefaultPersistentContextProvider;
import io.imiocode.persistence.PersistenceEvent;
import io.imiocode.persistence.PersistenceEventListener;
import io.imiocode.permission.PermissionChecker;
import io.imiocode.permission.PermissionCoordinator;
import io.imiocode.permission.PermissionGate;
import io.imiocode.permission.PermissionModePolicy;
import io.imiocode.permission.PermissionRequestFactory;
import io.imiocode.permission.PermissionSettings;
import io.imiocode.permission.RuntimePermissionSettings;
import io.imiocode.permission.command.RegexDangerousCommandDetector;
import io.imiocode.permission.command.RegexCommandRiskClassifier;
import io.imiocode.permission.command.ShellCommandScanner;
import io.imiocode.permission.command.ShellCommandTokenizer;
import io.imiocode.permission.command.StrictSafeCommandDetector;
import io.imiocode.permission.rule.PermissionRuleEngine;
import io.imiocode.permission.sandbox.WorkspacePathSandbox;
import io.imiocode.terminal.JLineTerminalUi;
import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiContext;
import io.imiocode.terminal.VersionResolver;
import io.imiocode.runtime.ConversationCoordinator;
import io.imiocode.runtime.ConversationLoop;
import io.imiocode.session.JsonlSessionStore;
import io.imiocode.session.SessionManager;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolLimits;
import io.imiocode.tool.ToolRegistry;
import io.imiocode.tool.ToolSelection;
import io.imiocode.tool.Tool;
import io.imiocode.tool.core.BashTool;
import io.imiocode.tool.core.EditFileTool;
import io.imiocode.tool.core.GlobTool;
import io.imiocode.tool.core.GrepTool;
import io.imiocode.tool.core.ReadFileTool;
import io.imiocode.tool.core.WriteFileTool;
import io.imiocode.tool.workspace.WorkspacePolicy;
import io.imiocode.skill.DefaultSkillForkRunner;
import io.imiocode.skill.LoadSkillTool;
import io.imiocode.skill.InstallSkillTool;
import io.imiocode.skill.SkillActivator;
import io.imiocode.skill.SkillCommandRegistrar;
import io.imiocode.skill.SkillCommandTool;
import io.imiocode.skill.SkillExecutor;
import io.imiocode.skill.SkillLoader;
import io.imiocode.skill.SkillManagementCommand;
import io.imiocode.skill.SkillSummaryFormatter;
import io.imiocode.skill.SkillParser;
import io.imiocode.skill.install.DefaultSkillInstaller;
import io.imiocode.skill.install.GitHubSkillFetcher;
import io.imiocode.skill.install.JdkSkillRemoteTransport;
import io.imiocode.skill.install.RemoteSkillLocator;
import io.imiocode.skill.install.SkillInstallListener;
import io.imiocode.skill.install.SkillInstallStage;
import io.imiocode.skill.install.SkillInstaller;
import io.imiocode.subagent.context.SubagentContextBuilder;
import io.imiocode.subagent.definition.AgentDefinitionLoader;
import io.imiocode.subagent.definition.AgentDefinition;
import io.imiocode.subagent.filter.SubagentToolFilter;
import io.imiocode.subagent.model.ModelAliasResolver;
import io.imiocode.subagent.runtime.AgentTool;
import io.imiocode.subagent.runtime.RunToCompletion;
import io.imiocode.subagent.runtime.SubagentAgentHandle;
import io.imiocode.subagent.runtime.SubagentAgentFactory;
import io.imiocode.subagent.runtime.SubagentDispatcher;
import io.imiocode.subagent.runtime.SubagentToolRegistryFactory;
import io.imiocode.subagent.task.TaskManager;
import io.imiocode.subagent.trace.TraceRegistry;
import io.imiocode.worktree.WorktreeException;
import io.imiocode.worktree.git.GitCommandRunner;
import io.imiocode.worktree.lifecycle.WorktreeManager;
import io.imiocode.worktree.runtime.LaunchOptions;
import io.imiocode.worktree.runtime.WorkspaceTransition;
import io.imiocode.worktree.runtime.WorkspaceTransitionController;
import io.imiocode.worktree.runtime.WorktreeBootstrap;
import io.imiocode.team.backend.*;
import io.imiocode.team.coordinator.*;
import io.imiocode.team.mailbox.MailboxStore;
import io.imiocode.team.model.*;
import io.imiocode.team.persistence.*;
import io.imiocode.team.runtime.*;
import io.imiocode.team.task.*;
import io.imiocode.team.tool.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ImioCode 主进程入口，负责装配配置、Provider、工具、持久上下文和终端会话。
 *
 * <p>工作区切换会重建与路径绑定的运行时资源；进程级资源则在最外层统一关闭，避免旧 Worktree 的
 * 工具、权限沙箱或会话状态泄漏到新工作区。</p>
 */
public final class ImioCodeApplication {
    private ImioCodeApplication() {
    }

    public static void main(String[] args) {
        int exitCode = TeamMemberProcess.requested(args) ? TeamMemberProcess.run(args) : run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run() {
        return run(new String[0]);
    }

    static int run(String[] args) {
        WorktreeManager worktrees = null;
        try {
            LaunchOptions options = LaunchOptions.parse(args);
            Path launchDirectory = Path.of("").toAbsolutePath().normalize();
            Path repositoryRoot = discoverRepositoryRoot(launchDirectory);
            if (repositoryRoot != null) {
                var worktreeConfig = new ConfigLoader().loadWorktrees(repositoryRoot);
                worktrees = new WorktreeManager(repositoryRoot, worktreeConfig);
            }
            var startup = new WorktreeBootstrap().resolve(launchDirectory, options, worktrees);
            Path workspace = startup.workspace();
            boolean showPendingResume = startup.pendingResumeNotice();
            while (true) {
                WorkspaceTransitionController transitions = new WorkspaceTransitionController();
                int exitCode = runWorkspace(workspace, worktrees, transitions, showPendingResume);
                if (exitCode != 0) return exitCode;
                WorkspaceTransition transition = transitions.current();
                if (transition instanceof WorkspaceTransition.Stay) return 0;
                if (transition instanceof WorkspaceTransition.Enter enter) workspace = enter.path();
                else if (transition instanceof WorkspaceTransition.Exit exit) workspace = exit.path();
                showPendingResume = false;
            }
        } catch (ConfigException exception) {
            System.err.println("[配置错误] " + exception.getMessage()); return 2;
        } catch (WorktreeException exception) {
            System.err.println("[Worktree错误] " + exception.getMessage()); return 2;
        } finally {
            if (worktrees != null) worktrees.close();
        }
    }

    private static int runWorkspace(Path workspace, WorktreeManager worktreeManager,
                                    WorkspaceTransitionController transitions,
                                    boolean showPendingResume) {
        LlmClient client = null;
        ConversationSession session = null;
        ConversationCoordinator coordinator = null;
        TerminalUi terminal = null;
        McpManager mcpManager = null;
        SkillInstaller remoteSkillInstaller = null;
        HookRuntime hookRuntime = HookRuntime.NOOP;
        HookContextFactory hookContexts = null;
        TaskManager taskManager = null;
        AgentTeamManager teamManager = null;
        try {
            workspace = workspace.toAbsolutePath().normalize();
            Path userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
            RuntimeConfig runtimeConfig = new ConfigLoader().loadAll(
                    workspace, userHome, System.getenv());
            AppConfig config = runtimeConfig.app();
            ToolLimits limits = ToolLimits.defaults();
            SecretRedactor redactor = runtimeConfig.redactor();
            CommandRegistry commandRegistry = createCommandRegistry();
            commandRegistry.register(new SkillManagementCommand());
            if (worktreeManager != null) commandRegistry.register(new WorktreeCommand(worktreeManager, transitions));
            commandRegistry.unregister("review");
            terminal = new JLineTerminalUi(redactor, config.ui().verbosity(), commandRegistry::complete);
            String version = VersionResolver.resolve();
            terminal.showWelcome(new UiContext(
                    "ImioCode",
                    version,
                    config.provider().configValue(),
                    config.model(),
                    workspace));
            if (showPendingResume) {
                terminal.printInfo("[Worktree] 检测到可恢复会话；使用 --resume 显式恢复");
            }
            for (ConfigNotice notice : runtimeConfig.notices()) {
                terminal.printInfo(notice.safeMessage());
            }
            for (var error : runtimeConfig.hooks().errors()) {
                String id = error.hookId().isBlank() ? "#" + error.index() : error.hookId();
                terminal.printError("[Hook/" + id + "] " + error.safeMessage());
            }
            hookContexts = new HookContextFactory(workspace);
            if (!runtimeConfig.hooks().hooks().isEmpty()) {
                HookTemplateResolver hookTemplates = new HookTemplateResolver();
                ActionDispatcher hookActions = new ActionDispatcher(java.util.List.of(
                        new CommandHookExecutor(hookTemplates, new JdkHookProcessRunner(), redactor),
                        new PromptHookExecutor(hookTemplates),
                        new HttpHookExecutor(hookTemplates, new JdkHookHttpTransport(), redactor),
                        new AgentPlaceholderHookExecutor()));
                hookRuntime = new DefaultHookEngine(runtimeConfig.hooks().hooks(),
                        new DefaultConditionEvaluator(), hookActions, redactor,
                        Clock.systemDefaultZone());
            }
            hookRuntime.runHooks(hookContexts.builder(HookEvent.STARTUP).build());
            HookRuntime configuredHooks = hookRuntime;
            HookContextFactory configuredHookContexts = hookContexts;
            HookToolLifecycleListener toolLifecycle = new HookToolLifecycleListener(
                    configuredHooks, configuredHookContexts);
            WorkspacePolicy policy = new WorkspacePolicy(workspace);
            PermissionSettings permissionSettings = runtimeConfig.permissions();
            RuntimePermissionSettings runtimePermissionSettings = new RuntimePermissionSettings(permissionSettings);
            WorkspacePathSandbox sandbox = new WorkspacePathSandbox(policy);
            ShellCommandScanner commandScanner = new ShellCommandScanner();
            ShellCommandTokenizer commandTokenizer = new ShellCommandTokenizer();
            StrictSafeCommandDetector safeCommandDetector = new StrictSafeCommandDetector(
                    workspace, commandScanner, commandTokenizer);
            RegexCommandRiskClassifier commandRiskClassifier = new RegexCommandRiskClassifier(
                    safeCommandDetector, commandScanner, commandTokenizer);
            PermissionChecker permissionChecker = new PermissionChecker(
                    workspace,
                    new RegexDangerousCommandDetector(),
                    sandbox,
                    new PermissionRuleEngine(),
                    new PermissionModePolicy(),
                    runtimePermissionSettings,
                    safeCommandDetector);
            PermissionGate permissionGate = new PermissionGate(
                    new PermissionRequestFactory(redactor, commandRiskClassifier),
                    permissionChecker,
                    new PermissionCoordinator());
            ToolRegistry registry = new ToolRegistry();
            registry.register(new ReadFileTool(policy, limits, redactor));
            registry.register(new WriteFileTool(policy, limits, redactor, toolLifecycle));
            registry.register(new EditFileTool(policy, limits, redactor, toolLifecycle));
            registry.register(new BashTool(policy, limits, redactor, toolLifecycle));
            registry.register(new GlobTool(policy, limits, redactor));
            registry.register(new GrepTool(policy, limits, redactor));

            Files.createDirectories(workspace.resolve(".imiocode").resolve("skills"));
            SkillLoader skillLoader = new SkillLoader(workspace, userHome);
            SkillActivator skillActivator = new SkillActivator(
                    registry,
                    spec -> new SkillCommandTool(spec, policy, limits, redactor));
            SkillExecutor skillExecutor = new SkillExecutor(skillLoader, skillActivator);
            registry.register(new LoadSkillTool(skillExecutor));
            SkillCommandRegistrar skillCommandRegistrar = new SkillCommandRegistrar(commandRegistry);
            skillCommandRegistrar.sync(skillLoader.snapshot());
            for (String diagnostic : skillLoader.snapshot().diagnostics()) {
                terminal.printError("[Skill] " + diagnostic);
            }
            JdkSkillRemoteTransport skillTransport = new JdkSkillRemoteTransport(runtimeConfig.skillInstall());
            TerminalUi skillTerminal = terminal;
            SkillInstallListener installProgress = (stage, message) -> {
                if (config.ui().verbosity() == UiVerbosity.VERBOSE
                        || stage == SkillInstallStage.DOWNLOADING
                        || stage == SkillInstallStage.COMPLETED) {
                    skillTerminal.printInfo("[Skill] " + message);
                }
            };
            remoteSkillInstaller = new DefaultSkillInstaller(
                    workspace,
                    runtimeConfig.skillInstall(),
                    new RemoteSkillLocator(runtimeConfig.skillInstall()),
                    new GitHubSkillFetcher(skillTransport),
                    new SkillParser(),
                    skillLoader,
                    () -> {
                        var snapshot = skillLoader.reload();
                        skillCommandRegistrar.sync(snapshot);
                        return snapshot;
                    });
            registry.register(new InstallSkillTool(remoteSkillInstaller, installProgress, limits, redactor));

            McpConfigLoadResult mcpConfigs = runtimeConfig.mcp();
            for (McpConfigError error : mcpConfigs.errors()) {
                terminal.printError(formatMcpError(error));
            }
            mcpManager = new McpManager(
                    new McpTransportFactory(new JsonRpcCodec()),
                    terminal,
                    terminal,
                    limits,
                    redactor,
                    version);
            McpStartupResult mcpStartup = mcpManager.start(mcpConfigs.servers().values(), registry);
            for (McpConfigError error : mcpStartup.errors()) {
                terminal.printError(formatMcpError(error));
            }
            if (!mcpConfigs.servers().isEmpty() || !mcpConfigs.errors().isEmpty()) {
                terminal.printInfo("[MCP] 已连接 " + mcpStartup.connectedServers()
                        + " 个 Server，注册 " + mcpStartup.registeredTools() + " 个工具");
            }

            AgentDefinitionLoader agentDefinitions = new AgentDefinitionLoader(workspace, userHome);
            for (String diagnostic : agentDefinitions.snapshot().diagnostics()) {
                terminal.printError("[Agent] " + diagnostic);
            }
            TraceRegistry traceRegistry = new TraceRegistry();
            String mainTraceId = traceRegistry.start("main", null, config.model(), false);
            SubagentToolFilter subagentToolFilter = new SubagentToolFilter(runtimeConfig.subagents());
            ModelAliasResolver modelAliases = new ModelAliasResolver(runtimeConfig.subagents());
            AtomicReference<ConversationSession> parentSession = new AtomicReference<>();
            SubagentToolRegistryFactory scopedTools = new SubagentToolRegistryFactory(registry, limits, redactor);
            Path activeWorkspace = workspace;
            SubagentAgentFactory childAgents = new SubagentAgentFactory() {
                @Override public SubagentAgentHandle create(AgentDefinition definition, ToolSelection selection) {
                    return createSubagentHandle(definition, selection, activeWorkspace, scopedTools,
                            config, modelAliases, permissionSettings, redactor,
                            configuredHooks, configuredHookContexts);
                }
                @Override public SubagentAgentHandle create(AgentDefinition definition, ToolSelection selection,
                                                             Path workdir) {
                    return createSubagentHandle(definition, selection, workdir, scopedTools,
                            config, modelAliases, permissionSettings, redactor,
                            configuredHooks, configuredHookContexts);
                }
            };
            RunToCompletion subagentRunner = new RunToCompletion(
                    registry, subagentToolFilter, new SubagentContextBuilder(),
                    childAgents, traceRegistry, mainTraceId, worktreeManager);
            taskManager = new TaskManager(subagentRunner,
                    runtimeConfig.subagents().maxBackgroundTasks(),
                    runtimeConfig.subagents().maxTaskRecords(),
                    runtimeConfig.subagents().notificationCapacity());
            TaskManager configuredTasks = taskManager;
            SubagentDispatcher subagentDispatcher = new SubagentDispatcher(
                    agentDefinitions, subagentRunner, configuredTasks,
                    () -> AgentHistoryContext.current().orElseGet(
                            () -> parentSession.get() == null ? List.of() : parentSession.get().historySnapshot()),
                    workspace);
            TeamPaths teamPaths = new TeamPaths(workspace);
            TeamStore teamStore = new TeamStore(teamPaths);
            MailboxStore mailboxStore = new MailboxStore(teamPaths, redactor,
                    runtimeConfig.teams().maxMessageChars(),
                    runtimeConfig.teams().maxMessagesPerMailbox(),
                    runtimeConfig.teams().maxTranscriptBytes());
            TranscriptStore transcriptStore = new TranscriptStore(teamPaths, redactor,
                    runtimeConfig.teams().maxMessageChars(),
                    runtimeConfig.teams().maxTranscriptBytes());
            TeamTaskStore teamTaskStore = new TeamTaskStore(teamPaths,
                    runtimeConfig.teams().maxTasksPerTeam());
            JdkProcessExecutor teamProcesses = new JdkProcessExecutor();
            InProcessBackend inProcessBackend = new InProcessBackend();
            BackendSelector backendSelector = new BackendSelector(List.of(
                    new TmuxBackend(teamProcesses, workspace),
                    new ITerm2Backend(teamProcesses, workspace, System.getProperty("os.name")),
                    inProcessBackend), System.getenv(), System.getProperty("os.name"),
                    runtimeConfig.teams().probeTimeout());
            TeamToolContext teamContext = new TeamToolContext();
            java.util.concurrent.atomic.AtomicReference<AgentTeamManager> teamManagerRef =
                    new java.util.concurrent.atomic.AtomicReference<>();
            TeamTaskService teamTaskService = new TeamTaskService(teamStore, teamTaskStore,
                    (team, member) -> {
                        AgentTeamManager manager = teamManagerRef.get();
                        if (manager != null) manager.stop(team, member);
                    });
            commandRegistry.register(new TaskCommand(configuredTasks, teamTaskService, teamContext::current));
            TeamMemberRunner teamRunner = (team, agentId, type, memberWorktree, prompt, transcript) -> {
                AgentDefinition definition = agentDefinitions.snapshot()
                        .find(type == null || type.isBlank() ? "general-purpose" : type)
                        .orElseThrow(() -> new IllegalArgumentException("未知团队 Agent 类型: " + type))
                        .withIsolation(io.imiocode.subagent.definition.AgentIsolation.NONE);
                TeamPrincipal memberPrincipal = new TeamPrincipal(team, agentId, TeamRole.MEMBER);
                java.util.function.Supplier<TeamPrincipal> memberIdentity = () -> memberPrincipal;
                List<Tool> memberTools = List.of(
                        new TaskCreateTool(teamTaskService, memberIdentity, limits, redactor),
                        new TaskGetTool(teamTaskService, memberIdentity, limits, redactor),
                        new TaskListTool(teamTaskService, memberIdentity, limits, redactor),
                        new TaskUpdateTool(teamTaskService, memberIdentity, limits, redactor),
                        new TaskStopTool(teamTaskService, memberIdentity, limits, redactor),
                        new SendMessageTool(teamManagerRef.get(), memberIdentity, limits, redactor));
                ToolSelection memberSelection = subagentToolFilter.selectTeamMember(
                        definition, registry.enabledNames());
                java.util.LinkedHashSet<String> allowed = new java.util.LinkedHashSet<>(memberSelection.allowedNames());
                memberTools.forEach(tool -> allowed.add(tool.definition().name()));
                memberSelection = ToolSelection.only(allowed);
                List<io.imiocode.conversation.ChatMessage> history = transcript.stream()
                        .filter(entry -> entry.role() == TranscriptRole.USER
                                || entry.role() == TranscriptRole.ASSISTANT)
                        .map(entry -> new io.imiocode.conversation.ChatMessage(
                                entry.role() == TranscriptRole.USER
                                        ? io.imiocode.conversation.MessageRole.USER
                                        : io.imiocode.conversation.MessageRole.ASSISTANT,
                                entry.content()))
                        .toList();
                String memberOutput;
                try (SubagentAgentHandle handle = createSubagentHandle(
                        definition, memberSelection, memberWorktree, scopedTools,
                        config, modelAliases, permissionSettings, redactor,
                        configuredHooks, configuredHookContexts, memberTools, true)) {
                    String roster = teamStore.require(team).members().values().stream()
                            .map(item -> item.agentId() + "(" + item.agentType() + ")")
                            .collect(java.util.stream.Collectors.joining(", "));
                    var result = handle.agent().run(new io.imiocode.agent.AgentRequest(
                            history,
                            new io.imiocode.conversation.ChatMessage(
                                    io.imiocode.conversation.MessageRole.USER, prompt),
                            List.of(new io.imiocode.conversation.SystemReminder(
                                    "你是团队 " + team + " 的成员 " + agentId
                                            + "。花名册：" + roster
                                            + "。通过 Task 工具和 SendMessage 协作；工作区是你的独立 Worktree。")),
                            memberSelection), event -> persistTeamToolEvent(
                                    transcriptStore, team, agentId, event,
                                    runtimeConfig.teams().maxMessageChars()));
                    if (!result.completed()) {
                        throw new IllegalStateException("团队成员未正常完成: " + result.stopReason());
                    }
                    memberOutput = result.finalResponse().orElseThrow().text();
                }
                // Agent 资源关闭可能中断其内部执行器；不能把该内部信号误当成成员停止请求。
                Thread.interrupted();
                return memberOutput;
            };
            teamManager = new AgentTeamManager(workspace, runtimeConfig.teams(), teamPaths,
                    teamStore, mailboxStore, transcriptStore, teamTaskStore,
                    backendSelector, worktreeManager, teamRunner);
            teamManagerRef.set(teamManager);
            AgentTeamManager configuredTeams = teamManager;
            TerminalUi teamTerminal = terminal;
            configuredTeams.recover().stream()
                    .max(java.util.Comparator.comparing(TeamConfig::updatedAt))
                    .ifPresent(restored -> {
                        teamContext.select(new TeamPrincipal(restored.name(),
                                restored.leadAgentId(), TeamRole.LEAD));
                        teamTerminal.printInfo("[Team] 已恢复团队 " + restored.name()
                                + "，成员 " + restored.members().size() + " 名");
                    });
            java.util.function.Supplier<TeamPrincipal> leadIdentity = teamContext::require;
            registry.register(new TeamCreateTool(configuredTeams, teamContext, limits, redactor));
            registry.register(new TeamConvergeTool(configuredTeams, teamContext, limits, redactor));
            registry.register(new TeamDeleteTool(configuredTeams, teamContext, limits, redactor));
            registry.register(new TaskCreateTool(teamTaskService, leadIdentity, limits, redactor));
            registry.register(new TaskGetTool(teamTaskService, leadIdentity, limits, redactor));
            registry.register(new TaskListTool(teamTaskService, leadIdentity, limits, redactor));
            registry.register(new TaskUpdateTool(teamTaskService, leadIdentity, limits, redactor));
            registry.register(new TaskStopTool(teamTaskService, leadIdentity, limits, redactor));
            registry.register(new SendMessageTool(configuredTeams, leadIdentity, limits, redactor));
            CoordinatorModeController coordinatorMode = new CoordinatorModeController(
                    runtimeConfig.teams().coordinatorEnabled(), System.getenv());
            registry.register(new CoordinatorModeTool(coordinatorMode, teamContext, limits, redactor));
            registry.register(new CoordinatorAdvanceTool(coordinatorMode, limits, redactor));
            registry.register(new AgentTool(subagentDispatcher, configuredTeams, teamContext, limits, redactor));
            java.util.Set<String> contextualTeamTools = java.util.Set.of(
                    "TeamDelete", "TeamConverge", "TaskCreate", "TaskGet", "TaskList",
                    "TaskUpdate", "TaskStop", "SendMessage");
            teamContext.configureLifecycle(
                    () -> contextualTeamTools.forEach(registry::enable),
                    () -> contextualTeamTools.forEach(registry::disable));

            PromptAssembler promptAssembler = new PromptAssembler(
                    SystemPromptBuilder.defaults(), registry);
            client = new LlmClientFactory().create(config, promptAssembler);
            ToolResultSpillStore spillStore = new ToolResultSpillStore(workspace, redactor);
            ToolResultOffloader offloader = new ToolResultOffloader(spillStore, redactor);
            ConversationSummarizer summarizer = new ConversationSummarizer(
                    client, new ConversationSerializer(), new SummaryParser());
            ContextManager contextManager = new ContextManager(
                    config.context(), config.maxOutputTokens(), promptAssembler,
                    new ApproximateTokenEstimator(), offloader, summarizer);
            Clock runtimeClock = Clock.systemDefaultZone();
            EnvironmentContextCollector environmentCollector = new EnvironmentContextCollector(
                    workspace,
                    runtimeClock,
                    Duration.ofSeconds(2),
                    config.model());
            Agent agent = new Agent(
                    client,
                    registry,
                    config.agent(),
                    config.maxOutputTokens(),
                    environmentCollector,
                    new EnvironmentReminderFormatter(),
                    permissionGate,
                    contextManager,
                    skillActivator,
                    true,
                    configuredHooks,
                    configuredHookContexts);
            LlmClient sharedClient = client;
            skillExecutor.setForkRunner(new DefaultSkillForkRunner(() -> new Agent(
                    sharedClient,
                    registry,
                    config.agent(),
                    config.maxOutputTokens(),
                    environmentCollector,
                    new EnvironmentReminderFormatter(),
                    permissionGate,
                    contextManager,
                    skillActivator,
                    false,
                    configuredHooks,
                    configuredHookContexts)));
            session = new ConversationSession(agent, new ConversationPolicy(coordinatorMode));
            parentSession.set(session);
            MarkdownMemoryStore memoryStore = new MarkdownMemoryStore(userHome, workspace);
            MemoryManager memoryManager = new MemoryManager(
                    memoryStore,
                    new MemorySafetyPolicy(config.memory(), redactor),
                    config.memory());
            TerminalUi eventTerminal = terminal;
            PersistenceEventListener persistenceEvents = event -> renderPersistenceEvent(eventTerminal, event);
            DefaultPersistentContextProvider persistentContext = new DefaultPersistentContextProvider(
                    new FileInstructionLoader(),
                    new InstructionLoadRequest(workspace, userHome, config.instructions()),
                    new InstructionReminderFormatter(),
                    memoryManager,
                    new MemoryReminderFormatter(),
                    memoryStore.path(MemoryScope.USER),
                    memoryStore.path(MemoryScope.PROJECT),
                    persistenceEvents);
            SessionManager sessionManager = config.sessions().enabled()
                    ? new SessionManager(new JsonlSessionStore(workspace, runtimeClock), config.sessions(), runtimeClock)
                    : null;
            coordinator = new ConversationCoordinator(
                    session,
                    sessionManager,
                    config.sessions(),
                    memoryManager,
                    config.memory(),
                    new LlmMemoryExtractor(client, config.memory(), new MemoryResponseParser(), redactor),
                    persistentContext,
                    persistenceEvents,
                    runtimeClock,
                    runtimePermissionSettings,
                    new ApproximateTokenEstimator(),
                    config.provider().configValue(),
                    config.model(),
                    workspace,
                    config.context().windowTokens(),
                    mcpStartup.connectedServers(),
                    mcpStartup.registeredTools(),
                    skillExecutor,
                    new SkillSummaryFormatter(),
                    skillCommandRegistrar,
                    remoteSkillInstaller,
                    configuredHooks,
                    configuredHookContexts);
            String permissionMode = permissionSettings.mode().name()
                    .toLowerCase(java.util.Locale.ROOT);
            if (config.ui().verbosity() == UiVerbosity.COMPACT) {
                terminal.printInfo("[权限] " + permissionMode
                        + " · /verbose 查看详细过程 · /exit 退出");
            } else {
                terminal.printInfo("输入 /exit 或 /quit 退出。");
                terminal.printInfo("[权限] 当前模式: " + permissionMode);
            }

            terminal.printInfo("[会话] " + coordinator.currentSession().id());
            new ConversationLoop(coordinator, terminal, commandRegistry, configuredTasks).run();
            return 0;
        } catch (ConfigException exception) {
            System.err.println("[配置错误] " + exception.getMessage());
            return 2;
        } catch (IOException exception) {
            System.err.println("[启动错误] 无法初始化终端");
            return 3;
        } catch (RuntimeException exception) {
            System.err.println("[运行错误] ImioCode 无法继续运行");
            return 1;
        } finally {
            if (teamManager != null) teamManager.close();
            if (taskManager != null) taskManager.close();
            if (coordinator != null) {
                coordinator.close();
            } else if (session != null) {
                session.close();
            } else if (client != null) {
                client.close();
            }
            if (mcpManager != null) {
                mcpManager.close();
            }
            if (coordinator == null && remoteSkillInstaller != null) {
                remoteSkillInstaller.close();
            }
            if (hookContexts != null) {
                try {
                    hookRuntime.runHooks(hookContexts.builder(HookEvent.SHUTDOWN).build());
                } catch (RuntimeException ignored) {
                    // 关闭流程不能被 Hook 失败阻塞。
                }
                if (terminal != null) {
                    hookRuntime.drainNotifications().forEach(terminal::showHookNotification);
                }
            }
            hookRuntime.close();
            if (terminal != null) {
                terminal.close();
            }
        }
    }

    static SubagentAgentHandle createSubagentHandle(
            AgentDefinition definition,
            ToolSelection selection,
            Path workdir,
            SubagentToolRegistryFactory scopedTools,
            AppConfig config,
            ModelAliasResolver modelAliases,
            PermissionSettings parentPermissions,
            SecretRedactor redactor,
            HookRuntime hooks,
            HookContextFactory ignoredParentHookContexts) {
        return createSubagentHandle(definition, selection, workdir, scopedTools, config,
                modelAliases, parentPermissions, redactor, hooks, ignoredParentHookContexts,
                List.of(), false);
    }

    static SubagentAgentHandle createSubagentHandle(
            AgentDefinition definition,
            ToolSelection selection,
            Path workdir,
            SubagentToolRegistryFactory scopedTools,
            AppConfig config,
            ModelAliasResolver modelAliases,
            PermissionSettings parentPermissions,
            SecretRedactor redactor,
            HookRuntime hooks,
            HookContextFactory ignoredParentHookContexts,
            List<? extends Tool> teamTools,
            boolean teamMember) {
        Path childWorkspace = workdir.toAbsolutePath().normalize();
        HookContextFactory childHookContexts = new HookContextFactory(childWorkspace);
        HookToolLifecycleListener childLifecycle = new HookToolLifecycleListener(hooks, childHookContexts);
        ToolRegistry childRegistry = teamMember
                ? scopedTools.createTeamMember(childWorkspace, childLifecycle, teamTools, selection)
                : scopedTools.create(childWorkspace, childLifecycle);
        var resolution = modelAliases.resolve(definition, config.model());
        AppConfig childConfig = config.withModel(resolution.model());
        PromptAssembler childPrompts = new PromptAssembler(SystemPromptBuilder.defaults(), childRegistry);
        LlmClient childClient = new LlmClientFactory().create(childConfig, childPrompts);
        WorkspacePolicy childPolicy = new WorkspacePolicy(childWorkspace);
        WorkspacePathSandbox childSandbox = new WorkspacePathSandbox(childPolicy);
        ShellCommandScanner childScanner = new ShellCommandScanner();
        ShellCommandTokenizer childTokenizer = new ShellCommandTokenizer();
        StrictSafeCommandDetector childSafe = new StrictSafeCommandDetector(
                childWorkspace, childScanner, childTokenizer);
        RegexCommandRiskClassifier childRisk = new RegexCommandRiskClassifier(
                childSafe, childScanner, childTokenizer);
        PermissionSettings childSettings = new PermissionSettings(
                definition.permissionMode(), parentPermissions.userRules(),
                parentPermissions.projectRules(), parentPermissions.localRules());
        PermissionChecker childChecker = new PermissionChecker(
                childWorkspace, new RegexDangerousCommandDetector(), childSandbox,
                new PermissionRuleEngine(), new PermissionModePolicy(), childSettings, childSafe);
        PermissionGate childGate = new PermissionGate(
                new PermissionRequestFactory(redactor, childRisk), childChecker,
                new PermissionCoordinator());
        ContextManager childContext = new ContextManager(
                childConfig.context(), childConfig.maxOutputTokens(), childPrompts,
                new ApproximateTokenEstimator(), offloaderFor(childWorkspace, redactor),
                new ConversationSummarizer(childClient, new ConversationSerializer(), new SummaryParser()));
        Agent childAgent = new Agent(childClient, childRegistry,
                new io.imiocode.config.AgentConfig(definition.maxTurns(), definition.timeout(),
                        config.agent().maxParallelTools()),
                childConfig.maxOutputTokens(), environmentCollectorFor(childWorkspace, resolution.model()),
                new EnvironmentReminderFormatter(), childGate, childContext, null,
                true, hooks, childHookContexts);
        return new SubagentAgentHandle(childAgent, resolution.model(), resolution.warning());
    }

    private static ToolResultOffloader offloaderFor(Path workspace, SecretRedactor redactor) {
        return new ToolResultOffloader(new ToolResultSpillStore(workspace, redactor), redactor);
    }

    private static void persistTeamToolEvent(TranscriptStore transcripts, String team, String agent,
                                             AgentEvent event, int maxChars) {
        if (!(event instanceof AgentEvent.ToolExecutionChanged changed)
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

    private static Path discoverRepositoryRoot(Path directory) {
        var result = new GitCommandRunner(Duration.ofSeconds(10)).run(
                directory, List.of("rev-parse", "--show-toplevel"));
        if (!result.success() || result.output().isBlank()) return null;
        try { return Path.of(result.output().strip()).toAbsolutePath().normalize(); }
        catch (RuntimeException exception) { return null; }
    }

    private static EnvironmentContextCollector environmentCollectorFor(Path workspace, String model) {
        return new EnvironmentContextCollector(workspace, Clock.systemDefaultZone(), Duration.ofSeconds(2), model);
    }

    private static String formatMcpError(McpConfigError error) {
        String server = error.serverName().isBlank() ? "" : "/" + error.serverName();
        return "[MCP" + server + "] " + error.safeMessage();
    }

    static CommandRegistry createCommandRegistry() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new HelpCommand());
        registry.register(new CompactCommand());
        registry.register(new ClearCommand());
        registry.register(new PlanCommand());
        registry.register(new DoCommand());
        registry.register(new SessionCommand());
        registry.register(new MemoryCommand());
        registry.register(new PermissionCommand());
        registry.register(new StatusCommand());
        registry.register(new ReviewCommand());
        registry.register(new ExitCommand());
        registry.register(new VerbosityCommand("verbose", UiVerbosity.VERBOSE));
        registry.register(new VerbosityCommand("compact-ui", UiVerbosity.COMPACT));
        return registry;
    }

    private static void renderPersistenceEvent(TerminalUi terminal, PersistenceEvent event) {
        if (event instanceof PersistenceEvent.SessionRestored restored) {
            terminal.printInfo("[会话] 已恢复 " + restored.id() + "，" + restored.messageCount() + " 条消息");
        } else if (event instanceof PersistenceEvent.SessionTailRecovered recovered) {
            terminal.printError("[会话] 已隔离损坏尾部：" + recovered.quarantinedTail().getFileName());
        } else if (event instanceof PersistenceEvent.MemoryUpdated updated) {
            if (updated.added() + updated.updated() > 0) {
                terminal.printInfo("[记忆] 新增 " + updated.added() + "，更新 " + updated.updated());
            }
        } else if (event instanceof PersistenceEvent.Warning warning) {
            terminal.printError(warning.safeMessage());
        }
    }
}
