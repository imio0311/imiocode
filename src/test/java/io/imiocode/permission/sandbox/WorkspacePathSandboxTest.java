package io.imiocode.permission.sandbox;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionRequest;
import io.imiocode.tool.ToolCall;
import io.imiocode.tool.ToolRisk;
import io.imiocode.tool.workspace.WorkspacePolicy;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspacePathSandboxTest {
    @TempDir
    Path workspace;

    @Test
    void allowsWorkspacePathAndRejectsEscapes() throws IOException {
        Files.writeString(workspace.resolve("safe.txt"), "ok");
        WorkspacePathSandbox sandbox =
                new WorkspacePathSandbox(new WorkspacePolicy(workspace));

        assertTrue(sandbox.inspect(request("read_file", "safe.txt")).allowed());
        assertTrue(sandbox.inspect(request("edit_file", "safe.txt")).allowed());
        assertTrue(sandbox.inspect(request("write_file", "new.txt")).allowed());
        assertTrue(sandbox.inspect(request("grep", ".")).allowed());
        assertTrue(sandbox.inspect(request("glob", "**/*.java")).allowed());
        assertFalse(sandbox.inspect(request("read_file", "../outside.txt")).allowed());
        assertFalse(sandbox.inspect(request(
                "read_file", workspace.resolve("safe.txt").toString())).allowed());
    }

    @Test
    void rejectsSymbolicLinkWhenSupported() throws IOException {
        Path outside = Files.createTempDirectory("imio-permission-outside-");
        Files.writeString(outside.resolve("secret.txt"), "secret");
        try {
            Files.createSymbolicLink(workspace.resolve("link"), outside);
        } catch (IOException | UnsupportedOperationException exception) {
            Assumptions.abort("当前平台不允许创建符号链接");
        }
        WorkspacePathSandbox sandbox =
                new WorkspacePathSandbox(new WorkspacePolicy(workspace));
        assertFalse(sandbox.inspect(request("read_file", "link/secret.txt")).allowed());
    }

    private static PermissionRequest request(String tool, String target) {
        return new PermissionRequest(
                new ToolCall("1", tool,
                        JsonNodeFactory.instance.objectNode().put("path", target)),
                ToolRisk.LOW,
                PermissionOperation.READ,
                target.replace('\\', '/'),
                target);
    }
}
