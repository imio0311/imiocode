package io.imiocode.permission;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.tool.ToolRisk;

/** 允许动态工具把真正执行目标交给统一权限链。 */
public interface PermissionTargetProvider {
    PermissionOperation permissionOperation();

    String permissionTarget(ObjectNode arguments);

    default ToolRisk permissionRisk(ObjectNode arguments, ToolRisk fallback) {
        return fallback;
    }
}
