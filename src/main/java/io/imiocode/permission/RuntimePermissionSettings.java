package io.imiocode.permission;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** 仅在当前进程内切换模式，规则始终复用启动时的不可变快照。 */
public final class RuntimePermissionSettings implements PermissionSettingsProvider {
    private final PermissionSettings initial;
    private final AtomicReference<PermissionMode> mode;

    public RuntimePermissionSettings(PermissionSettings initial) {
        this.initial = Objects.requireNonNull(initial, "initial");
        this.mode = new AtomicReference<>(initial.mode());
    }

    public PermissionMode mode() {
        return mode.get();
    }

    public void switchMode(PermissionMode next) {
        mode.set(Objects.requireNonNull(next, "权限模式不能为空"));
    }

    @Override
    public PermissionSettings snapshot() {
        return new PermissionSettings(
                mode.get(), initial.userRules(), initial.projectRules(), initial.localRules());
    }
}
