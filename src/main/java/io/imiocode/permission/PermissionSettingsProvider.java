package io.imiocode.permission;

/** 为一次权限检查提供不可变设置快照。 */
@FunctionalInterface
public interface PermissionSettingsProvider {
    PermissionSettings snapshot();
}
