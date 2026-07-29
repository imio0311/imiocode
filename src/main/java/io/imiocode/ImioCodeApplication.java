package io.imiocode;

import io.imiocode.agent.Agent;
import io.imiocode.config.AppConfig;
import io.imiocode.config.ConfigException;
import io.imiocode.config.ConfigLoader;
import io.imiocode.conversation.ConversationLoop;
import io.imiocode.conversation.ConversationSession;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmClientFactory;
import io.imiocode.prompt.EnvironmentContextCollector;
import io.imiocode.prompt.EnvironmentReminderFormatter;
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
        try {
            Path workspace = Path.of("").toAbsolutePath().normalize();
            AppConfig config = new ConfigLoader().load(workspace, System.getenv());
            ToolLimits limits = ToolLimits.defaults();
            SecretRedactor redactor = new SecretRedactor(config.apiKey());
            WorkspacePolicy policy = new WorkspacePolicy(workspace);
            ToolRegistry registry = new ToolRegistry();
            registry.register(new ReadFileTool(policy, limits, redactor));
            registry.register(new WriteFileTool(policy, limits, redactor));
            registry.register(new EditFileTool(policy, limits, redactor));
            registry.register(new BashTool(policy, limits, redactor));
            registry.register(new GlobTool(policy, limits, redactor));
            registry.register(new GrepTool(policy, limits, redactor));

            client = new LlmClientFactory().create(config, registry);
            Agent agent = new Agent(
                    client,
                    registry,
                    config.agent(),
                    config.maxOutputTokens(),
                    new EnvironmentContextCollector(
                            workspace,
                            Clock.systemDefaultZone(),
                            Duration.ofSeconds(2)),
                    new EnvironmentReminderFormatter());
            session = new ConversationSession(agent);
            terminal = new JLineTerminalUi(redactor);
            terminal.showWelcome(new UiContext(
                    "ImioCode",
                    VersionResolver.resolve(),
                    config.provider().configValue(),
                    config.model(),
                    workspace));
            terminal.printInfo("输入 /exit 或 /quit 退出。");

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
            if (terminal != null) {
                terminal.close();
            }
            if (session != null) {
                session.close();
            } else if (client != null) {
                client.close();
            }
        }
    }
}
