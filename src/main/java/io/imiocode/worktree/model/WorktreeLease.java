package io.imiocode.worktree.model;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/** 子 Agent 的独占 Worktree；closeSafely 幂等并执行安全清理。 */
public final class WorktreeLease implements AutoCloseable {
    private final WorktreeSession session;
    private final Path lockPath;
    private final FileChannel channel;
    private final FileLock lock;
    private final Function<WorktreeLease, WorktreeCleanupReport> cleanup;
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile WorktreeCleanupReport report;

    public WorktreeLease(WorktreeSession session, Path lockPath, FileChannel channel, FileLock lock,
                         Function<WorktreeLease, WorktreeCleanupReport> cleanup) {
        this.session = Objects.requireNonNull(session); this.lockPath = Objects.requireNonNull(lockPath);
        this.channel = Objects.requireNonNull(channel); this.lock = Objects.requireNonNull(lock);
        this.cleanup = Objects.requireNonNull(cleanup);
    }

    public WorktreeSession session() { return session; }
    public Path workdir() { return session.worktreePath(); }
    public Path lockPath() { return lockPath; }

    public WorktreeCleanupReport closeSafely() {
        if (closed.compareAndSet(false, true)) {
            try {
                report = cleanup.apply(this);
            } finally {
                // 即使清理检查本身异常，也绝不能遗留进程级文件锁。
                release();
            }
        }
        return report;
    }

    public void release() {
        try { if (lock.isValid()) lock.release(); } catch (java.io.IOException ignored) { }
        try { channel.close(); } catch (java.io.IOException ignored) { }
        try { java.nio.file.Files.deleteIfExists(lockPath); } catch (java.io.IOException ignored) { }
    }

    @Override public void close() { closeSafely(); }
}
