package io.imiocode.persistence;

import io.imiocode.config.InstructionsConfig;
import io.imiocode.config.MemoryConfig;
import io.imiocode.instruction.FileInstructionLoader;
import io.imiocode.instruction.InstructionLoadRequest;
import io.imiocode.instruction.InstructionReminderFormatter;
import io.imiocode.memory.MarkdownMemoryStore;
import io.imiocode.memory.MemoryManager;
import io.imiocode.memory.MemoryReminderFormatter;
import io.imiocode.memory.MemorySafetyPolicy;
import io.imiocode.memory.MemoryScope;
import io.imiocode.tool.SecretRedactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPersistentContextProviderTest {
    @TempDir Path root;

    @Test
    void ordersSourcesAndReloadsChangedFilesOnNextSnapshot() throws Exception {
        Path workspace = Files.createDirectory(root.resolve("workspace"));
        Path userHome = Files.createDirectory(root.resolve("home"));
        Files.createDirectories(userHome.resolve(".imiocode"));
        Files.writeString(userHome.resolve(".imiocode/MEWCODE.md"), "用户指令-v1");
        Files.writeString(workspace.resolve("MEWCODE.md"), "项目指令-v1");

        MemoryConfig config = MemoryConfig.defaults();
        MarkdownMemoryStore store = new MarkdownMemoryStore(userHome, workspace);
        MemoryManager manager = new MemoryManager(store,
                new MemorySafetyPolicy(config, new SecretRedactor("")), config);
        manager.add(MemoryScope.USER, "用户偏好简洁回答");
        manager.add(MemoryScope.PROJECT, "项目使用 Java 21");
        DefaultPersistentContextProvider provider = new DefaultPersistentContextProvider(
                new FileInstructionLoader(),
                new InstructionLoadRequest(workspace, userHome, InstructionsConfig.defaults()),
                new InstructionReminderFormatter(), manager, new MemoryReminderFormatter(),
                store.path(MemoryScope.USER), store.path(MemoryScope.PROJECT), PersistenceEventListener.noop());

        var first = provider.currentReminders();
        assertEquals(4, first.size());
        assertTrue(first.get(0).content().contains("用户指令-v1"));
        assertTrue(first.get(1).content().contains("项目指令-v1"));
        assertTrue(first.get(2).content().contains("用户偏好简洁回答"));
        assertTrue(first.get(3).content().contains("项目使用 Java 21"));

        Files.writeString(workspace.resolve("MEWCODE.md"), "项目指令-v2-and-longer");
        var refreshed = provider.currentReminders();
        assertTrue(refreshed.get(1).content().contains("项目指令-v2-and-longer"));
    }
}
