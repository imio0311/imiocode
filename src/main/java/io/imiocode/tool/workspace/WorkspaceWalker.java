package io.imiocode.tool.workspace;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.BooleanSupplier;

/** 不跟随链接、顺序稳定且有扫描上限的深度优先遍历。 */
public final class WorkspaceWalker {
    private final WorkspacePolicy policy;

    public WorkspaceWalker(WorkspacePolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public WalkResult walk(Path start, int maxScannedPaths, BooleanSupplier cancelled)
            throws IOException, InterruptedException {
        if (maxScannedPaths <= 0) {
            throw new IllegalArgumentException("扫描上限必须为正数");
        }
        if (!start.toAbsolutePath().normalize().startsWith(policy.workspace())) {
            throw new IllegalArgumentException("扫描起点超出工作区");
        }
        List<Path> discovered = new ArrayList<>();
        Deque<Path> pending = new ArrayDeque<>();
        pending.push(start);
        int scanned = 0;
        boolean truncated = false;

        while (!pending.isEmpty()) {
            if (cancelled.getAsBoolean()) {
                throw new InterruptedException("扫描已取消");
            }
            Path directory = pending.pop();
            Children childrenResult = sortedChildren(directory, maxScannedPaths - scanned);
            List<Path> children = childrenResult.paths();
            List<Path> childDirectories = new ArrayList<>();
            for (Path child : children) {
                if (cancelled.getAsBoolean()) {
                    throw new InterruptedException("扫描已取消");
                }
                if (scanned >= maxScannedPaths) {
                    truncated = true;
                    break;
                }
                scanned++;
                if (!policy.isAllowedDiscoveredPath(child)) {
                    continue;
                }
                discovered.add(child);
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    childDirectories.add(child);
                }
            }
            if (truncated) {
                break;
            }
            if (childrenResult.overflow()) {
                truncated = true;
                break;
            }
            for (int index = childDirectories.size() - 1; index >= 0; index--) {
                pending.push(childDirectories.get(index));
            }
        }
        return new WalkResult(List.copyOf(discovered), scanned, truncated);
    }

    private Children sortedChildren(Path directory, int limit) throws IOException {
        Comparator<Path> order = Comparator.comparing(policy::relativeUnixPath);
        PriorityQueue<Path> smallest = new PriorityQueue<>(Math.max(1, limit), order.reversed());
        boolean overflow = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (smallest.size() < limit) {
                    smallest.add(path);
                } else {
                    overflow = true;
                    if (limit > 0 && order.compare(path, smallest.peek()) < 0) {
                        smallest.poll();
                        smallest.add(path);
                    }
                }
            }
        }
        List<Path> paths = new ArrayList<>(smallest);
        paths.sort(order);
        return new Children(paths, overflow);
    }

    public record WalkResult(List<Path> paths, int scannedPaths, boolean truncated) {
    }

    private record Children(List<Path> paths, boolean overflow) {
    }
}
