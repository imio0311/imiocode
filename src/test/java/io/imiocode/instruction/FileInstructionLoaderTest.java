package io.imiocode.instruction;

import io.imiocode.config.InstructionsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileInstructionLoaderTest {
    @TempDir Path root;

    @Test
    void loadsUserAndProjectSourcesInPriorityOrder() throws Exception {
        Path workspace = Files.createDirectory(root.resolve("workspace"));
        Path userHome = Files.createDirectory(root.resolve("home"));
        Files.createDirectories(userHome.resolve(".imiocode"));
        Files.writeString(userHome.resolve(".imiocode/MEWCODE.md"), "用户偏好：中文回答");
        Files.writeString(workspace.resolve("shared.md"), "项目事实：Java 21");
        Files.writeString(workspace.resolve("MEWCODE.md"), "@include shared.md\n项目规则：先测试");

        InstructionSnapshot snapshot = new FileInstructionLoader().load(new InstructionLoadRequest(
                workspace, userHome, InstructionsConfig.defaults()));

        assertEquals(2, snapshot.sources().size());
        assertEquals(InstructionScope.USER, snapshot.sources().get(0).scope());
        assertEquals(InstructionScope.PROJECT, snapshot.sources().get(1).scope());
        assertTrue(snapshot.sources().get(1).expandedContent().contains("Java 21"));
        assertTrue(snapshot.dependencies().stream().anyMatch(path -> path.endsWith("shared.md")));
    }

    @Test
    void isolatesBrokenSourceAndKeepsOtherSources() throws Exception {
        Path workspace = Files.createDirectory(root.resolve("workspace"));
        Path userHome = Files.createDirectory(root.resolve("home"));
        Files.createDirectories(userHome.resolve(".imiocode"));
        Files.writeString(userHome.resolve(".imiocode/MEWCODE.md"), "用户规则");
        Files.writeString(workspace.resolve("MEWCODE.md"), "@include ../outside.md");

        InstructionSnapshot snapshot = new FileInstructionLoader().load(new InstructionLoadRequest(
                workspace, userHome, InstructionsConfig.defaults()));

        assertEquals(1, snapshot.sources().size());
        assertEquals(1, snapshot.problems().size());
        assertTrue(snapshot.problems().getFirst().safeMessage().contains("不允许 .."));
    }

    @Test
    void disabledLoaderDoesNotReadFiles() throws Exception {
        Path workspace = Files.createDirectory(root.resolve("workspace"));
        Path userHome = Files.createDirectory(root.resolve("home"));
        Files.writeString(workspace.resolve("MEWCODE.md"), "不会加载");

        InstructionSnapshot snapshot = new FileInstructionLoader().load(new InstructionLoadRequest(
                workspace, userHome, new InstructionsConfig(false, 8, 131072)));

        assertTrue(snapshot.sources().isEmpty());
        assertTrue(snapshot.dependencies().isEmpty());
    }
}
