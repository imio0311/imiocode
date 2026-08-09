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
import java.util.List;

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
    void defaultsToAutoEditAndFailsOnUnknownFields() throws IOException {
        Path workspace = Files.createDirectory(root.resolve("work-default"));
        Path home = Files.createDirectory(root.resolve("home-default"));
        assertEquals(PermissionMode.AUTO_EDIT,
                new PermissionRuleLoader().load(workspace, home).mode());

        Files.createDirectories(workspace.resolve(".imiocode"));
        Files.writeString(workspace.resolve(".imiocode/permissions.yaml"), "unknown: true");
        assertThrows(ConfigException.class,
                () -> new PermissionRuleLoader().load(workspace, home));
    }

    @Test
    void loadsUnifiedRulesAsOrderedProjectRules() {
        PermissionConfigDocument document = new PermissionConfigDocument(
                "auto-edit",
                List.of(
                        new PermissionConfigDocument.RuleDocument("allow", "read_*", "src/**"),
                        new PermissionConfigDocument.RuleDocument("ask", "bash", null)));

        PermissionSettings settings = new PermissionRuleLoader().loadUnified(document);

        assertEquals(PermissionMode.AUTO_EDIT, settings.mode());
        assertEquals(List.of(), settings.userRules());
        assertEquals(2, settings.projectRules().size());
        assertEquals(PermissionAction.ALLOW, settings.projectRules().getFirst().action());
        assertEquals(PermissionAction.ASK, settings.projectRules().get(1).action());
        assertEquals(List.of(), settings.localRules());
    }

    @Test
    void unifiedEmptyDocumentUsesSafeDefaults() {
        PermissionSettings settings = new PermissionRuleLoader().loadUnified(
                new PermissionConfigDocument(null, null));

        assertEquals(PermissionMode.AUTO_EDIT, settings.mode());
        assertEquals(List.of(), settings.projectRules());
    }

    @Test
    void rejectsInvalidUnifiedRuleAsOneConfiguration() {
        PermissionConfigDocument document = new PermissionConfigDocument(
                "ask",
                List.of(new PermissionConfigDocument.RuleDocument("allow", "", null)));

        assertThrows(ConfigException.class,
                () -> new PermissionRuleLoader().loadUnified(document));
    }
}
