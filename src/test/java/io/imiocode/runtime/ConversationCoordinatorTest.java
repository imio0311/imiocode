package io.imiocode.runtime;

import io.imiocode.agent.AgentMode;
import io.imiocode.config.MemoryConfig;
import io.imiocode.config.SessionsConfig;
import io.imiocode.context.ApproximateTokenEstimator;
import io.imiocode.conversation.ChatRequest;
import io.imiocode.conversation.ChatResponse;
import io.imiocode.conversation.ConversationSession;
import io.imiocode.llm.LlmClient;
import io.imiocode.llm.LlmException;
import io.imiocode.llm.StreamListener;
import io.imiocode.memory.MemoryExtractionResult;
import io.imiocode.memory.MemoryManager;
import io.imiocode.permission.PermissionMode;
import io.imiocode.permission.PermissionSettings;
import io.imiocode.permission.RuntimePermissionSettings;
import io.imiocode.persistence.PersistentContextProvider;
import io.imiocode.conversation.SystemReminder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationCoordinatorTest {
    @TempDir Path workspace;

    @Test
    void rejectsMutableCommandsWhileAgentIsActiveAndBuildsSafeStatus() throws Exception {
        BlockingClient client = new BlockingClient();
        MemoryConfig memoryConfig = new MemoryConfig(false, false, false, false, 1, 1, 1, 1);
        RuntimePermissionSettings permissions = new RuntimePermissionSettings(PermissionSettings.defaults());
        ConversationCoordinator coordinator = new ConversationCoordinator(
                new ConversationSession(client),
                null,
                new SessionsConfig(false, 0, 0),
                new MemoryManager(null, null, memoryConfig),
                memoryConfig,
                (user, history, memories) -> new MemoryExtractionResult(List.of(), List.of()),
                emptyContext(),
                event -> { },
                Clock.systemUTC(),
                permissions,
                new ApproximateTokenEstimator(),
                "deepseek",
                "deepseek-chat",
                workspace,
                64_000,
                1,
                2);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var running = executor.submit(() -> coordinator.sendWithEvents("正在执行", text -> { }));
            assertTrue(client.started.await(2, TimeUnit.SECONDS));

            assertThrows(IllegalStateException.class, () -> coordinator.switchMode(AgentMode.PLAN));
            assertThrows(IllegalStateException.class,
                    () -> coordinator.switchPermissionMode(PermissionMode.FULL_ACCESS));
            assertThrows(IllegalStateException.class, coordinator::newSession);
            assertEquals(AgentMode.DO, coordinator.mode());
            assertEquals(PermissionMode.ASK, coordinator.permissionMode());

            client.release.countDown();
            running.get(2, TimeUnit.SECONDS);
            coordinator.switchMode(AgentMode.PLAN);
            coordinator.switchPermissionMode(PermissionMode.AUTO_EDIT);
            var status = coordinator.status();
            assertEquals(AgentMode.PLAN, status.agentMode());
            assertEquals(PermissionMode.AUTO_EDIT, status.permissionMode());
            assertEquals(1, status.connectedMcpServers());
            assertEquals(2, status.registeredMcpTools());
            assertTrue(status.estimatedTokens() > 0);
        } finally {
            client.release.countDown();
            coordinator.close();
            executor.shutdownNow();
        }
    }

    private static PersistentContextProvider emptyContext() {
        return new PersistentContextProvider() {
            @Override public List<SystemReminder> currentReminders() { return List.of(); }
            @Override public void reloadInstructions() { }
            @Override public void reloadMemories() { }
        };
    }

    private static final class BlockingClient implements LlmClient {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public ChatResponse streamChat(ChatRequest request, StreamListener listener) throws LlmException {
            started.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) throw new IllegalStateException("测试等待超时");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("测试被中断", exception);
            }
            listener.onTextDelta("完成");
            return new ChatResponse("完成");
        }

        @Override public void close() { }
    }
}
