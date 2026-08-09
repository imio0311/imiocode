package io.imiocode.worktree.lifecycle;

import io.imiocode.worktree.WorktreeException;
import io.imiocode.worktree.config.WorktreeConfig;
import io.imiocode.worktree.git.GitCommandRunner;
import io.imiocode.worktree.git.GitWorktreeClient;
import io.imiocode.worktree.model.ManagedWorktree;
import io.imiocode.worktree.model.WorktreeChangeSummary;
import io.imiocode.worktree.model.WorktreeCleanupReport;
import io.imiocode.worktree.model.WorktreeCreation;
import io.imiocode.worktree.model.WorktreeLease;
import io.imiocode.worktree.model.WorktreeSession;
import io.imiocode.worktree.persistence.WorktreeSessionStore;
import io.imiocode.worktree.security.WorktreeNames;
import io.imiocode.worktree.security.WorktreeSlugValidator;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** 受管 Worktree 的唯一生命周期入口。 */
public final class WorktreeManager implements AutoCloseable {
    private final Path originalRoot;
    private final WorktreeConfig config;
    private final WorktreeSlugValidator slugs;
    private final WorktreeNames names;
    private final GitWorktreeClient git;
    private final WorktreeSessionStore sessions;
    private final WorktreePostCreationSetup setup;
    private final WorktreeSafetyInspector safety;
    private final Clock clock;
    private final Path lockDirectory;
    private final Set<String> activeLeases = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService cleaner;
    private final AtomicBoolean closed = new AtomicBoolean();

    public WorktreeManager(Path originalRoot, WorktreeConfig config) {
        this(originalRoot, config, Clock.systemUTC());
    }

    public WorktreeManager(Path originalRoot, WorktreeConfig config, Clock clock) {
        this.originalRoot = originalRoot.toAbsolutePath().normalize(); this.config = config; this.clock = clock;
        this.slugs = new WorktreeSlugValidator();
        this.names = new WorktreeNames(this.originalRoot, config, slugs);
        this.git = new GitWorktreeClient(this.originalRoot, new GitCommandRunner(config.gitTimeout()));
        this.sessions = new WorktreeSessionStore(this.originalRoot);
        this.setup = new WorktreePostCreationSetup(this.originalRoot, config, git);
        this.safety = new WorktreeSafetyInspector(names, git);
        this.lockDirectory = this.originalRoot.resolve(".imiocode").resolve("worktree-locks");
        try {
            Files.createDirectories(names.managedRoot());
            Path repositoryReal = this.originalRoot.toRealPath();
            Path managedReal = names.managedRoot().toRealPath();
            if (!managedReal.startsWith(repositoryReal) || managedReal.equals(repositoryReal)) {
                throw new WorktreeException("Worktree 受管目录的真实路径越界");
            }
            Files.createDirectories(lockDirectory);
            Path lockReal = lockDirectory.toRealPath();
            if (!lockReal.startsWith(repositoryReal) || lockReal.equals(repositoryReal)) {
                throw new WorktreeException("Worktree 锁目录的真实路径越界");
            }
        } catch (IOException exception) {
            throw new WorktreeException("无法创建 Worktree 受管目录", exception);
        }
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "imiocode-worktree-cleaner"); thread.setDaemon(true); return thread;
        });
        long interval = Math.max(1, config.cleanupInterval().toSeconds());
        cleaner.scheduleWithFixedDelay(this::cleanupStaleQuietly, interval, interval, TimeUnit.SECONDS);
    }

    public synchronized WorktreeCreation create(String inputSlug) {
        ensureOpen(); String slug = slugs.validate(inputSlug); Path path = names.path(slug); String branch = names.branch(slug);
        Optional<ManagedWorktree> existing = list().stream().filter(item -> item.slug().equals(slug)).findFirst();
        if (existing.isPresent()) throw new WorktreeException("Worktree 已存在: " + slug);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) throw new WorktreeException("Worktree 目录已存在但未受 Git 管理");
        String originalHead = git.head(); String originalBranch = git.branch(); boolean branchExisted = git.branchExists(branch);
        boolean added = false;
        try {
            Files.createDirectories(path.getParent());
            if (branchExisted) git.addExisting(path, branch); else git.addNew(path, branch, originalHead);
            added = true;
            List<String> warnings = setup.apply(path);
            return new WorktreeCreation(new WorktreeSession(UUID.randomUUID().toString(), slug, originalRoot,
                    path, branch, originalBranch, originalHead, clock.instant()), warnings);
        } catch (RuntimeException | IOException exception) {
            if (added) { try { git.removeForce(path); } catch (RuntimeException ignored) { } }
            if (!branchExisted && git.branchExists(branch)) { try { git.deleteBranch(branch); } catch (RuntimeException ignored) { } }
            if (exception instanceof WorktreeException value) throw value;
            throw new WorktreeException("无法创建 Worktree", exception);
        }
    }

    public synchronized WorktreeSession enter(String inputSlug) {
        ensureOpen();
        if (sessions.exists()) throw new WorktreeException("已有活动 Worktree 会话，不能嵌套 Enter");
        String slug = slugs.validate(inputSlug);
        ManagedWorktree found = list().stream().filter(item -> item.slug().equals(slug)).findFirst()
                .orElseThrow(() -> new WorktreeException("Worktree 不存在: " + slug));
        WorktreeSession session = new WorktreeSession(UUID.randomUUID().toString(), slug, originalRoot,
                found.path(), found.branch(), git.branch(), git.head(), clock.instant());
        safety.validateIdentity(session); sessions.save(session); return session;
    }

    public synchronized WorktreeSession resume() {
        ensureOpen(); WorktreeSession session = sessions.load()
                .orElseThrow(() -> new WorktreeException("没有可恢复的 Worktree 会话"));
        if (!samePath(session.originalCwd(), originalRoot)) throw new WorktreeException("恢复记录不属于当前仓库");
        safety.inspect(session); return session;
    }

    public synchronized WorktreeCleanupReport exit(boolean remove, boolean discardChanges) {
        ensureOpen(); WorktreeSession session = sessions.load()
                .orElseThrow(() -> new WorktreeException("当前没有活动 Worktree 会话"));
        if (!remove) {
            safety.validateIdentity(session); sessions.clear();
            return WorktreeCleanupReport.retained(session.worktreePath(), session.worktreeBranch(), "已保留 Worktree");
        }
        WorktreeCleanupReport report = destroy(session, discardChanges);
        sessions.clear(); return report;
    }

    public synchronized WorktreeCleanupReport remove(String inputSlug, boolean discardChanges) {
        ensureOpen(); String slug = slugs.validate(inputSlug);
        Optional<WorktreeSession> active = sessions.load();
        if (active.isPresent() && active.orElseThrow().slug().equals(slug)) {
            throw new WorktreeException("不能直接删除活动 Worktree，请使用 /worktree exit remove");
        }
        ManagedWorktree found = list().stream().filter(item -> item.slug().equals(slug)).findFirst()
                .orElseThrow(() -> new WorktreeException("Worktree 不存在: " + slug));
        WorktreeSession session = new WorktreeSession(UUID.randomUUID().toString(), slug, originalRoot,
                found.path(), found.branch(), git.branch(), git.head(), clock.instant());
        return destroy(session, discardChanges);
    }

    public synchronized List<ManagedWorktree> list() {
        ensureOpen(); String activeSlug = sessions.load().map(WorktreeSession::slug).orElse("");
        String base = git.head(); List<ManagedWorktree> result = new ArrayList<>();
        for (var entry : git.list()) {
            if (!entry.path().startsWith(names.managedRoot()) || !entry.branch().startsWith(WorktreeNames.BRANCH_PREFIX)) continue;
            String slug = entry.branch().substring(WorktreeNames.BRANCH_PREFIX.length());
            try {
                if (!names.path(slug).equals(entry.path())) continue;
                String status = git.statusPorcelain(entry.path());
                int changed = status.isBlank() ? 0 : (int) status.lines().filter(line -> !line.isBlank()).count();
                int commits = git.uniqueCommitCount(base, entry.branch());
                result.add(new ManagedWorktree(slug, entry.path(), entry.branch(), entry.head(),
                        slug.equals(activeSlug), new WorktreeChangeSummary(changed, commits,
                        git.branchContainedByRemote(entry.branch()))));
            } catch (RuntimeException ignored) {
                // 无法安全检查的条目不对外伪造健康状态；删除路径仍会 fail closed。
            }
        }
        return result.stream().sorted(Comparator.comparing(ManagedWorktree::slug)).toList();
    }

    public WorktreeLease createAgentWorktree(String agentName) {
        ensureOpen(); String slug = slugs.uniqueAgentSlug(agentName); Path lockPath = lockDirectory.resolve(slug + ".lock");
        FileChannel channel = null; FileLock lock = null;
        try {
            channel = FileChannel.open(lockPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            if (lock == null || !activeLeases.add(slug)) throw new WorktreeException("无法获取子 Agent Worktree 锁");
            WorktreeCreation created = create(slug);
            return new WorktreeLease(created.session(), lockPath, channel, lock, this::cleanupLease);
        } catch (IOException | RuntimeException exception) {
            activeLeases.remove(slug);
            try { if (lock != null && lock.isValid()) lock.release(); } catch (IOException ignored) { }
            try { if (channel != null) channel.close(); } catch (IOException ignored) { }
            try { Files.deleteIfExists(lockPath); } catch (IOException ignored) { }
            if (exception instanceof WorktreeException value) throw value;
            throw new WorktreeException("无法创建子 Agent Worktree 锁", exception);
        }
    }

    public synchronized List<WorktreeCleanupReport> cleanupStaleNow() {
        ensureOpen(); List<WorktreeCleanupReport> reports = new ArrayList<>(); Instant cutoff = clock.instant().minus(config.staleAfter());
        for (ManagedWorktree item : list()) {
            if (!item.slug().startsWith("agent-") || item.active() || activeLeases.contains(item.slug())
                    || hasLiveLeaseLock(item.slug())) continue;
            try {
                Instant modified = Files.getLastModifiedTime(item.path(), LinkOption.NOFOLLOW_LINKS).toInstant();
                if (modified.isAfter(cutoff)) continue;
                WorktreeSession session = new WorktreeSession(UUID.randomUUID().toString(), item.slug(), originalRoot,
                        item.path(), item.branch(), git.branch(), git.head(), modified);
                WorktreeChangeSummary changes = safety.inspect(session);
                if (changes.clean()) reports.add(destroy(session, false));
                else reports.add(WorktreeCleanupReport.retained(item.path(), item.branch(), changes.summary()));
            } catch (RuntimeException | IOException exception) {
                reports.add(WorktreeCleanupReport.retained(item.path(), item.branch(), "安全检查失败"));
            }
        }
        cleanupOrphanLocks();
        try { git.prune(); } catch (RuntimeException ignored) { }
        return List.copyOf(reports);
    }

    public boolean hasRecoverableSession() { return sessions.exists(); }
    public Path originalRoot() { return originalRoot; }
    public WorktreeSessionStore sessionStore() { return sessions; }

    private synchronized WorktreeCleanupReport cleanupLease(WorktreeLease lease) {
        String slug = lease.session().slug();
        try {
            WorktreeChangeSummary changes = safety.inspect(lease.session());
            if (!changes.clean()) return WorktreeCleanupReport.retained(lease.workdir(),
                    lease.session().worktreeBranch(), changes.summary());
            return destroy(lease.session(), false);
        } catch (RuntimeException exception) {
            return WorktreeCleanupReport.retained(lease.workdir(), lease.session().worktreeBranch(), "安全检查失败");
        } finally { activeLeases.remove(slug); }
    }

    private WorktreeCleanupReport destroy(WorktreeSession session, boolean discardChanges) {
        WorktreeChangeSummary changes = safety.inspect(session);
        if (!changes.clean() && !discardChanges) {
            throw new WorktreeException("Worktree 包含 " + changes.summary() + "，需要明确确认丢弃");
        }
        git.removeForce(session.worktreePath());
        RuntimeException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try { git.deleteBranch(session.worktreeBranch()); last = null; break; }
            catch (RuntimeException exception) {
                last = exception;
                try { Thread.sleep(100L); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt(); throw new WorktreeException("删除 Worktree 已取消", interrupted);
                }
            }
        }
        if (last != null) throw new WorktreeException("Worktree 已移除但分支删除失败", last);
        return WorktreeCleanupReport.removed(session.worktreePath(), session.worktreeBranch(), List.of());
    }

    private void cleanupStaleQuietly() { try { cleanupStaleNow(); } catch (RuntimeException ignored) { } }

    private void cleanupOrphanLocks() {
        try (var paths = Files.list(lockDirectory)) {
            for (Path path : paths.filter(value -> value.getFileName().toString().endsWith(".lock")).toList()) {
                String slug = path.getFileName().toString().replaceFirst("\\.lock$", "");
                if (activeLeases.contains(slug)) continue;
                try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE);
                     FileLock ignored = channel.tryLock()) {
                    if (ignored != null) Files.deleteIfExists(path);
                } catch (IOException | java.nio.channels.OverlappingFileLockException ignored) { }
            }
        } catch (IOException ignored) { }
    }

    /** 跨进程也必须尊重子 Agent lease；锁状态不可判断时按 fail-closed 处理。 */
    private boolean hasLiveLeaseLock(String slug) {
        Path path = lockDirectory.resolve(slugs.validate(slug) + ".lock");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return false;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
            try (FileLock acquired = channel.tryLock()) {
                return acquired == null;
            }
        } catch (java.nio.channels.OverlappingFileLockException exception) {
            return true;
        } catch (IOException | RuntimeException exception) {
            return true;
        }
    }

    private void ensureOpen() { if (closed.get()) throw new WorktreeException("WorktreeManager 已关闭"); }
    private static boolean samePath(Path left, Path right) {
        try { return Files.isSameFile(left, right); } catch (IOException exception) { return left.equals(right); }
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        cleaner.shutdownNow();
        try { cleaner.awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}
