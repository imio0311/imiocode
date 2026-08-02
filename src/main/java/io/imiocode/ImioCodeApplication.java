package io.imiocode;

import io.imiocode.agent.Agent;
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
import io.imiocode.conversation.ConversationLoop;
import io.imiocode.conversation.ConversationSession;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmClientFactory;
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
import io.imiocode.permission.PermissionChecker;
import io.imiocode.permission.PermissionCoordinator;
import io.imiocode.permission.PermissionGate;
import io.imiocode.permission.PermissionModePolicy;
import io.imiocode.permission.PermissionRequestFactory;
import io.imiocode.permission.PermissionSettings;
import io.imiocode.permission.command.RegexDangerousCommandDetector;
import io.imiocode.permission.command.StrictSafeCommandDetector;
import io.imiocode.permission.rule.PermissionRuleEngine;
import io.imiocode.permission.sandbox.WorkspacePathSandbox;
import io.imiocode.terminal.JLineTerminalUi;
import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiContext;
import io.imiocode.terminal.VersionResolver;
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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

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
        TerminalUi terminal = null;
        McpManager mcpManager = null;
        try {
            Path workspace = Path.of("").toAbsolutePath().normalize();
            Path userHome = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
            RuntimeConfig runtimeConfig = new ConfigLoader().loadAll(
                    workspace, userHome, System.getenv());
            AppConfig config = runtimeConfig.app();
            ToolLimits limits = ToolLimits.defaults();
            SecretRedactor redactor = runtimeConfig.redactor();
            terminal = new JLineTerminalUi(redactor, config.ui().verbosity());
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
            WorkspacePolicy policy = new WorkspacePolicy(workspace);
            PermissionSettings permissionSettings = runtimeConfig.permissions();
            WorkspacePathSandbox sandbox = new WorkspacePathSandbox(policy);
            PermissionChecker permissionChecker = new PermissionChecker(
                    workspace,
                    new RegexDangerousCommandDetector(),
                    sandbox,
                    new PermissionRuleEngine(),
                    new PermissionModePolicy(),
                    permissionSettings,
                    new StrictSafeCommandDetector(workspace));
            PermissionGate permissionGate = new PermissionGate(
                    new PermissionRequestFactory(redactor),
                    permissionChecker,
                    new PermissionCoordinator());
            ToolRegistry registry = new ToolRegistry();
            registry.register(new ReadFileTool(policy, limits, redactor));
            registry.register(new WriteFileTool(policy, limits, redactor));
            registry.register(new EditFileTool(policy, limits, redactor));
            registry.register(new BashTool(policy, limits, redactor));
            registry.register(new GlobTool(policy, limits, redactor));
            registry.register(new GrepTool(policy, limits, redactor));

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
            Agent agent = new Agent(
                    client,
                    registry,
                    config.agent(),
                    config.maxOutputTokens(),
                    new EnvironmentContextCollector(
                            workspace,
                            Clock.systemDefaultZone(),
                            Duration.ofSeconds(2),
                            config.model()),
                    new EnvironmentReminderFormatter(),
                    permissionGate,
                    contextManager);
            session = new ConversationSession(agent);
            String permissionMode = permissionSettings.mode().name()
                    .toLowerCase(java.util.Locale.ROOT);
            if (config.ui().verbosity() == UiVerbosity.COMPACT) {
                terminal.printInfo("[权限] " + permissionMode
                        + " · /verbose 查看详细过程 · /exit 退出");
            } else {
                terminal.printInfo("输入 /exit 或 /quit 退出。");
                terminal.printInfo("[权限] 当前模式: " + permissionMode);
            }

            new ConversationLoop(session, terminal).run();
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
            if (session != null) {
                session.close();
            } else if (client != null) {
                client.close();
            }
            if (mcpManager != null) {
                mcpManager.close();
            }
            if (terminal != null) {
                terminal.close();
            }
        }
    }

    private static String formatMcpError(McpConfigError error) {
        String server = error.serverName().isBlank() ? "" : "/" + error.serverName();
        return "[MCP" + server + "] " + error.safeMessage();
    }
}
