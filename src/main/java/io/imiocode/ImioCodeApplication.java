package io.imiocode;

import io.imiocode.config.AppConfig;
import io.imiocode.config.ConfigException;
import io.imiocode.config.ConfigLoader;
import io.imiocode.conversation.ConversationLoop;
import io.imiocode.conversation.ConversationSession;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmClientFactory;
import io.imiocode.terminal.JLineTerminalUi;
import io.imiocode.terminal.TerminalUi;
import io.imiocode.terminal.UiContext;
import io.imiocode.terminal.VersionResolver;

import java.io.IOException;
import java.nio.file.Path;

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
        TerminalUi terminal = null;
        try {
            AppConfig config = new ConfigLoader().load(System.getenv());
            client = new LlmClientFactory().create(config);
            terminal = new JLineTerminalUi();
            terminal.showWelcome(new UiContext(
                    "ImioCode",
                    VersionResolver.resolve(),
                    config.provider().configValue(),
                    config.model(),
                    Path.of("")));
            terminal.printInfo("输入 /exit 或 /quit 退出。");

            ConversationSession session = new ConversationSession(client);
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
            if (client != null) {
                client.close();
            }
        }
    }
}
