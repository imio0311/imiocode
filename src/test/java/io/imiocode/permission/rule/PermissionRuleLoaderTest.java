package io.imiocode.permission.rule;

import io.imiocode.config.ConfigException;
import io.imiocode.permission.PermissionAction;
import io.imiocode.permission.PermissionMode;
import io.imiocode.permission.PermissionSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionRuleLoaderTest {
    @TempDir
    Path root;

    @Test
    void loadsThreeLayersAndUsesHighestMode() throws IOException {
        Path workspace = Files.createDirectory(root.resolve("work"));
        Path home = Files.createDirectory(root.resolve("home"));
        Files.createDirectories(home.resolve(".imiocode"));
        Files.createDirectories(workspace.resolve(".imiocode"));
        Files.writeString(home.resolve(".imiocode/permissions.yaml"), """
                mode: read-only
                rules:
                  - action: deny
                    tool: bash
                """);
        Files.writeString(workspace.resolve(".imiocode/permissions.yaml"), """
                mode: full-access
                rules:
                  - action: allow
                    tool: read_*
                    target: src/**
                """);
        Files.writeString(workspace.resolve(".imiocode/permissions.local.yaml"), """
                rules:
                  - action: ask
                    tool: write_file
                """);

        PermissionSettings settings = new PermissionRuleLoader().load(workspace, home);

        assertEquals(PermissionMode.READ_ONLY, settings.mode());
        assertEquals(PermissionAction.DENY, settings.userRules().getFirst().action());
        assertEquals(1, settings.projectRules().size());
        assertEquals(1, settings.localRules().size());
    }

    @Test
    void defaultsToAskAndFailsOnUnknownFields() throws IOException {
        Path workspace = Files.createDirectory(root.resolve("work-default"));
        Path home = Files.createDirectory(root.resolve("home-default"));
        assertEquals(PermissionMode.ASK,
                new PermissionRuleLoader().load(workspace, home).mode());

        Files.createDirectories(workspace.resolve(".imiocode"));
        Files.writeString(workspace.resolve(".imiocode/permissions.yaml"), "unknown: true");
        assertThrows(ConfigException.class,
                () -> new PermissionRuleLoader().load(workspace, home));
    }
}
