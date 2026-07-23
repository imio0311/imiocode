package io.imiocode.tool.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** 统一约束所有文件工具只能访问工作区内的普通、非敏感路径。 */
public final class WorkspacePolicy {
    private final Path workspace;

    public WorkspacePolicy(Path workspace) {
        Objects.requireNonNull(workspace, "workspace");
        Path normalized = workspace.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS) || isLinkLike(normalized)) {
            throw new IllegalArgumentException("工作区必须是存在的非链接目录");
        }
        this.workspace = normalized;
    }

    public Path workspace() {
        return workspace;
    }

    public Path resolveExistingFile(String input) {
        Path target = resolveInput(input);
        validateSegments(target);
        if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("文件不存在或不是普通文件");
        }
        return target;
    }

    public Path resolveExistingPath(String input) {
        if (".".equals(input)) {
            return workspace;
        }
        Path target = resolveInput(input);
        validateSegments(target);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("路径不存在");
        }
        if (!Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("路径不是普通文件或目录");
        }
        return target;
    }

    public Path resolveWritableFile(String input) {
        Path target = resolveInput(input);
        validateSegments(target);
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("目标父目录不存在");
        }
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("目标不是普通文件");
        }
        return target;
    }

    public Path revalidateWritable(Path target) {
        Objects.requireNonNull(target, "target");
        Path normalized = target.toAbsolutePath().normalize();
        if (!normalized.startsWith(workspace) || normalized.equals(workspace)) {
            throw new IllegalArgumentException("目标路径超出工作区");
        }
        validateSensitive(workspace.relativize(normalized));
        validateSegments(normalized);
        return normalized;
    }

    public boolean isAllowedDiscoveredPath(Path path) {
        try {
            Path normalized = path.toAbsolutePath().normalize();
            if (!normalized.startsWith(workspace) || normalized.equals(workspace)) {
                return false;
            }
            validateSensitive(workspace.relativize(normalized));
            validateSegments(normalized);
            return Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)
                    || Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public String relativeUnixPath(Path path) {
        return workspace.relativize(path.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
    }

    private Path resolveInput(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        Path relative;
        try {
            relative = Path.of(input);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("路径格式无效");
        }
        if (relative.isAbsolute() || relative.getRoot() != null) {
            throw new IllegalArgumentException("只允许工作区相对路径");
        }
        for (Path segment : relative) {
            if ("..".equals(segment.toString())) {
                throw new IllegalArgumentException("路径不能包含 ..");
            }
        }
        Path target = workspace.resolve(relative).normalize();
        if (!target.startsWith(workspace) || target.equals(workspace)) {
            throw new IllegalArgumentException("目标路径超出工作区");
        }
        validateSensitive(workspace.relativize(target));
        return target;
    }

    private void validateSensitive(Path relative) {
        for (Path segment : relative) {
            String name = segment.toString().toLowerCase(Locale.ROOT);
            if (name.equals(".git")
                    || name.equals("config.yaml")
                    || name.equals(".env")
                    || name.startsWith(".env.")) {
                throw new IllegalArgumentException("不允许访问受保护路径");
            }
        }
    }

    private void validateSegments(Path target) {
        Path current = workspace;
        Path relative = workspace.relativize(target);
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && isLinkLike(current)) {
                throw new IllegalArgumentException("不允许访问链接或重解析点");
            }
        }
    }

    private static boolean isLinkLike(Path path) {
        if (Files.isSymbolicLink(path)) {
            return true;
        }
        try {
            return Files.readAttributes(
                    path,
                    java.nio.file.attribute.BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS).isOther();
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法验证路径安全性");
        }
    }
}
