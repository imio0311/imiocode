package io.imiocode;

import io.imiocode.agent.Agent;
import io.imiocode.agent.AgentHistoryContext;
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
import io.imiocode.subagent.filter.SubagentToolFilter;
import io.imiocode.subagent.model.ModelAliasResolver;
import io.imiocode.subagent.runtime.AgentTool;
import io.imiocode.subagent.runtime.RunToCompletion;
import io.imiocode.subagent.runtime.SubagentAgentHandle;
import io.imiocode.subagent.runtime.SubagentDispatcher;
import io.imiocode.subagent.task.TaskManager;
import io.imiocode.subagent.trace.TraceRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ImioCodeApplication {
    private ImioCodeApplication() {
    }

    public static void main(String[] args) {
        int exitCode = run();
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run() {
        LlmClient client = null;
        ConversationSession session = null;
        ConversationCoordinator coordinator = null;
        TerminalUi terminal = null;
        McpManager mcpManager = null;
        SkillInstaller remoteSkillInstaller = null;
        HookRuntime hookRuntime = HookRuntime.NOOP;
        HookContextFactory hookContexts = null;
        TaskManager taskManager = null;
        try {
            Path workspace = Path.of("").toAbsolutePath().normalize();
            Path userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
            RuntimeConfig runtimeConfig = new ConfigLoader().loadAll(
                    workspace, userHome, System.getenv());
            AppConfig config = runtimeConfig.app();
            ToolLimits limits = ToolLimits.defaults();
            SecretRedactor redactor = runtimeConfig.redactor();
            CommandRegistry commandRegistry = createCommandRegistry();
            commandRegistry.register(new SkillManagementCommand());
            commandRegistry.unregister("review");
            terminal = new JLineTerminalUi(redactor, config.ui().verbosity(), commandRegistry::complete);
            String version = VersionResolver.resolve();
            terminal.showWelcome(new UiContext(
                    "ImioCode",
                    version,
                    config.provider().configValue(),
                    config.model(),
                    workspace));
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
            RunToCompletion subagentRunner = new RunToCompletion(
                    registry, subagentToolFilter, new SubagentContextBuilder(),
                    (definition, selection) -> {
                        var resolution = modelAliases.resolve(definition, config.model());
                        AppConfig childConfig = config.withModel(resolution.model());
                        PromptAssembler childPrompts = new PromptAssembler(SystemPromptBuilder.defaults(), registry);
                        LlmClient childClient = new LlmClientFactory().create(childConfig, childPrompts);
                        PermissionSettings childSettings = new PermissionSettings(
                                definition.permissionMode(), permissionSettings.userRules(),
                                permissionSettings.projectRules(), permissionSettings.localRules());
                        PermissionChecker childChecker = new PermissionChecker(
                                workspace, new RegexDangerousCommandDetector(), sandbox,
                                new PermissionRuleEngine(), new PermissionModePolicy(), childSettings,
                                safeCommandDetector);
                        PermissionGate childGate = new PermissionGate(
                                new PermissionRequestFactory(redactor, commandRiskClassifier),
                                childChecker, new PermissionCoordinator());
                        ContextManager childContext = new ContextManager(
                                childConfig.context(), childConfig.maxOutputTokens(), childPrompts,
                                new ApproximateTokenEstimator(), offloaderFor(workspace, redactor),
                                new ConversationSummarizer(childClient, new ConversationSerializer(), new SummaryParser()));
                        Agent childAgent = new Agent(childClient, registry,
                                new io.imiocode.config.AgentConfig(definition.maxTurns(), definition.timeout(),
                                        config.agent().maxParallelTools()),
                                childConfig.maxOutputTokens(), environmentCollectorFor(workspace, config.model()),
                                new EnvironmentReminderFormatter(), childGate, childContext, null,
                                true, configuredHooks, configuredHookContexts);
                        return new SubagentAgentHandle(childAgent, resolution.model(), resolution.warning());
                    }, traceRegistry, mainTraceId);
            taskManager = new TaskManager(subagentRunner,
                    runtimeConfig.subagents().maxBackgroundTasks(),
                    runtimeConfig.subagents().maxTaskRecords(),
                    runtimeConfig.subagents().notificationCapacity());
            TaskManager configuredTasks = taskManager;
            commandRegistry.register(new TaskCommand(configuredTasks));
            SubagentDispatcher subagentDispatcher = new SubagentDispatcher(
                    agentDefinitions, subagentRunner, configuredTasks,
                    () -> AgentHistoryContext.current().orElseGet(
                            () -> parentSession.get() == null ? List.of() : parentSession.get().historySnapshot()),
                    workspace);
            registry.register(new AgentTool(subagentDispatcher, limits, redactor));

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
            session = new ConversationSession(agent);
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
            if (taskManager != null) taskManager.close();
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

    private static ToolResultOffloader offloaderFor(Path workspace, SecretRedactor redactor) {
        return new ToolResultOffloader(new ToolResultSpillStore(workspace, redactor), redactor);
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
