package io.imiocode.permission;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimePermissionSettingsTest {
    @Test
    void switchesAllModesWithoutChangingRules() {
        List<PermissionRule> rules = List.of(new PermissionRule(
                PermissionRuleLayer.USER, PermissionAction.ALLOW, "read_file", Optional.empty()));
        RuntimePermissionSettings runtime = new RuntimePermissionSettings(new PermissionSettings(
                PermissionMode.ASK, rules, List.of(), List.of()));

        for (PermissionMode mode : PermissionMode.values()) {
            runtime.switchMode(mode);
            PermissionSettings snapshot = runtime.snapshot();
            assertEquals(mode, snapshot.mode());
            assertEquals(rules, snapshot.userRules());
        }
        assertThrows(NullPointerException.class, () -> runtime.switchMode(null));
    }
}
