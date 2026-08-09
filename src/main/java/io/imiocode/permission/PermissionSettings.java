package io.imiocode.permission;

import java.util.List;
import java.util.Objects;

/** 一次会话使用的不可变权限设置快照。 */
public record PermissionSettings(
        PermissionMode mode,
        List<PermissionRule> userRules,
        List<PermissionRule> projectRules,
        List<PermissionRule> localRules
) {
    public PermissionSettings {
        mode = Objects.requireNonNullElse(mode, PermissionMode.AUTO_EDIT);
        userRules = List.copyOf(Objects.requireNonNullElse(userRules, List.of()));
        projectRules = List.copyOf(Objects.requireNonNullElse(projectRules, List.of()));
        localRules = List.copyOf(Objects.requireNonNullElse(localRules, List.of()));
    }

    public static PermissionSettings defaults() {
        return new PermissionSettings(PermissionMode.AUTO_EDIT, List.of(), List.of(), List.of());
    }
}
