package io.imiocode.permission;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** 允许动态工具把真正执行目标交给统一权限链。 */
public interface PermissionTargetProvider {
    PermissionOperation permissionOperation();

    String permissionTarget(ObjectNode arguments);
}
