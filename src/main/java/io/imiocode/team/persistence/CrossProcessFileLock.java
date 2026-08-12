package io.imiocode.team.persistence;

import io.imiocode.team.TeamException;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** JVM 内互斥与 OS FileLock 组合，保护跨成员进程的读改写事务。 */
public final class CrossProcessFileLock {
    private static final ConcurrentHashMap<Path, ReentrantLock> JVM_LOCKS = new ConcurrentHashMap<>();

    private CrossProcessFileLock() { }

    public static <T> T withLock(Path lockFile, Supplier<T> operation) {
        Path normalized = lockFile.toAbsolutePath().normalize();
        ReentrantLock jvmLock = JVM_LOCKS.computeIfAbsent(normalized, ignored -> new ReentrantLock());
        jvmLock.lock();
        try {
            Files.createDirectories(normalized.getParent());
            try (FileChannel channel = FileChannel.open(normalized,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var ignored = channel.lock()) {
                return operation.get();
            }
        } catch (IOException exception) {
            throw new TeamException("无法锁定团队数据", exception);
        } finally {
            jvmLock.unlock();
            // 不从表中移除：解锁与 remove 之间若另一线程刚取得旧锁，第三线程会创建新锁并并发进入。
            // 团队锁路径受配置上限约束，保留稳定锁对象可换取严格的进程内互斥。
        }
    }
}
