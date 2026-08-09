package io.imiocode.permission.sandbox;

import io.imiocode.permission.PermissionOperation;
import io.imiocode.permission.PermissionRequest;
import io.imiocode.tool.workspace.WorkspacePolicy;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** 与核心文件工具共享 WorkspacePolicy 语义的路径沙箱。 */
public final class WorkspacePathSandbox implements PathSandbox {
    private final WorkspacePolicy policy;

    public WorkspacePathSandbox(WorkspacePolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy 不能为空");
    }

    @Override
    public SandboxResult inspect(PermissionRequest request) {
        Objects.requireNonNull(request, "request 不能为空");
        if (request.operation() == PermissionOperation.COMMAND) {
            return SandboxResult.allow();
        }
        String target = request.normalizedTarget();
        try {
            validateRelativeSyntax(target);
            switch (request.call().name().toLowerCase(Locale.ROOT)) {
                case "read_file", "edit_file" -> policy.resolveExistingFile(target);
                case "write_file" -> policy.resolveWritableFile(target);
                case "install_skill" -> policy.resolveWritableFile(target);
                case "grep" -> policy.resolveExistingPath(target);
                case "agent", "load_skill" -> {
                    // 调度与 Skill 加载本身没有调用方文件路径；内部真实工具仍逐次经过沙箱。
                }
                case "glob" -> {
                    // Glob 始终遍历工作区；这里只验证 pattern 本身不能表达越界根。
                }
                default -> throw new IllegalArgumentException("未知文件工具");
            }
            return SandboxResult.allow();
        } catch (RuntimeException exception) {
            return SandboxResult.deny("目标未通过工作区沙箱验证");
        }
    }

    @Override
    public Path revalidateWritable(Path target) {
        return policy.revalidateWritable(target);
    }

    private static void validateRelativeSyntax(String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        String normalized = target.replace('\\', '/');
        if (normalized.startsWith("/")
                || normalized.startsWith("//")
                || normalized.matches("(?i)^[a-z]:/.*")) {
            throw new IllegalArgumentException("只允许工作区相对路径");
        }
        for (String segment : normalized.split("/")) {
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("路径不能包含 ..");
            }
        }
    }
}
