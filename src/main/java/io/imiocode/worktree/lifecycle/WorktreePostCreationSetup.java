package io.imiocode.worktree.lifecycle;

import io.imiocode.worktree.config.WorktreeConfig;
import io.imiocode.worktree.git.GitWorktreeClient;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 创建后的本地配置、hooks、依赖链接和 include 设置。 */
public final class WorktreePostCreationSetup {
    private static final List<String> LOCAL_CONFIG = List.of(
            "config.yaml", ".imiocode/mcp.local.yaml", ".imiocode/permissions.local.yaml");
    private final Path originalRoot;
    private final WorktreeConfig config;
    private final GitWorktreeClient git;

    public WorktreePostCreationSetup(Path originalRoot, WorktreeConfig config, GitWorktreeClient git) {
        this.originalRoot = Objects.requireNonNull(originalRoot).toAbsolutePath().normalize();
        this.config = Objects.requireNonNull(config); this.git = Objects.requireNonNull(git);
    }

    /** hooks 失败会抛出；其他平台相关设置按 best-effort 返回警告。 */
    public List<String> apply(Path worktree) {
        List<String> warnings = new ArrayList<>();
        if (config.copyLocalConfig()) LOCAL_CONFIG.forEach(value -> copyOne(value, worktree, warnings));
        Path hooks = resolveHooksDirectory();
        git.configureWorktreeHooks(worktree, hooks);
        for (String directory : config.linkDirectories()) linkDependency(directory, worktree, warnings);
        copyIncludes(worktree, warnings);
        return List.copyOf(warnings);
    }

    private Path resolveHooksDirectory() {
        Path common = Path.of(new io.imiocode.worktree.git.GitCommandRunner(config.gitTimeout())
                .checked(originalRoot, List.of("rev-parse", "--git-common-dir"), "无法定位 Git hooks"));
        if (!common.isAbsolute()) common = originalRoot.resolve(common);
        return common.normalize().resolve("hooks");
    }

    private void copyOne(String relative, Path worktree, List<String> warnings) {
        Path source = originalRoot.resolve(relative).normalize(); Path target = worktree.resolve(relative).normalize();
        if (!source.startsWith(originalRoot) || !target.startsWith(worktree)) return;
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            Files.createDirectories(target.getParent());
            Path worktreeReal = worktree.toRealPath();
            Path parentReal = target.getParent().toRealPath();
            if (!parentReal.startsWith(worktreeReal)
                    || (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                    && (Files.isSymbolicLink(target)
                    || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)))) {
                warnings.add("拒绝复制到越界或非普通文件 " + relative);
                return;
            }
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        }
        catch (IOException exception) { warnings.add("未能复制本地文件 " + relative); }
    }

    private void linkDependency(String relative, Path worktree, List<String> warnings) {
        Path source = originalRoot.resolve(relative).normalize(); Path target = worktree.resolve(relative).normalize();
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS) || Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return;
        try { Files.createSymbolicLink(target, source); }
        catch (IOException | UnsupportedOperationException | SecurityException exception) {
            warnings.add("当前平台无法链接依赖目录 " + relative);
        }
    }

    private void copyIncludes(Path worktree, List<String> warnings) {
        for (String pattern : config.copyIncludes()) {
            java.nio.file.PathMatcher matcher = originalRoot.getFileSystem().getPathMatcher("glob:" + pattern);
            try {
                Files.walkFileTree(originalRoot, java.util.Set.of(), 32, new SimpleFileVisitor<>() {
                    @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (dir.equals(originalRoot.resolve(".git")) || dir.startsWith(originalRoot.resolve(config.directory())))
                            return FileVisitResult.SKIP_SUBTREE;
                        return FileVisitResult.CONTINUE;
                    }
                    @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        Path relative = originalRoot.relativize(file);
                        if (attrs.isRegularFile() && !Files.isSymbolicLink(file) && matcher.matches(relative)
                                && git.isIgnored(relative)) {
                            copyOne(relative.toString(), worktree, warnings);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException | RuntimeException exception) { warnings.add("复制 include 失败: " + pattern); }
        }
    }
}
