package io.imiocode.skill.install;

import io.imiocode.skill.FileSkillSource;
import io.imiocode.skill.LoadedSkill;
import io.imiocode.skill.SkillCatalogSnapshot;
import io.imiocode.skill.SkillDescriptor;
import io.imiocode.skill.SkillLoader;
import io.imiocode.skill.SkillOrigin;
import io.imiocode.skill.SkillParser;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 下载、完整校验、原子安装、热刷新和失败回滚的事务实现。 */
public final class DefaultSkillInstaller implements SkillInstaller {
    private final Path projectRoot;
    private final SkillInstallConfig config;
    private final RemoteSkillLocator locator;
    private final RemoteSkillFetcher fetcher;
    private final SkillParser parser;
    private final SkillLoader loader;
    private final SkillInstallRefresher refresher;
    private final AtomicReference<SkillDownloadBudget> activeBudget = new AtomicReference<>();
    private final AtomicReference<Thread> activeThread = new AtomicReference<>();
    private final AtomicBoolean busy = new AtomicBoolean();

    public DefaultSkillInstaller(
            Path workspace,
            SkillInstallConfig config,
            RemoteSkillLocator locator,
            RemoteSkillFetcher fetcher,
            SkillParser parser,
            SkillLoader loader,
            SkillInstallRefresher refresher) {
        this.projectRoot = Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize()
                .resolve(".imiocode").resolve("skills");
        this.config = Objects.requireNonNull(config, "config");
        this.locator = Objects.requireNonNull(locator, "locator");
        this.fetcher = Objects.requireNonNull(fetcher, "fetcher");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.loader = Objects.requireNonNull(loader, "loader");
        this.refresher = Objects.requireNonNull(refresher, "refresher");
    }

    @Override
    public SkillInstallResult install(SkillInstallRequest request, SkillInstallListener listener) {
        Objects.requireNonNull(request, "request");
        SkillInstallListener checkedListener = listener == null ? SkillInstallListener.NOOP : listener;
        if (!busy.compareAndSet(false, true)) throw new SkillInstallException("已有 Skill 安装任务正在执行");
        SkillDownloadBudget budget = new SkillDownloadBudget(config);
        activeBudget.set(budget);
        activeThread.set(Thread.currentThread());
        Path temporary = null;
        Path backup = null;
        Path target = null;
        boolean installed = false;
        List<SkillInstallStage> stages = new ArrayList<>();
        try {
            emit(stages, checkedListener, SkillInstallStage.QUEUED, "Skill 安装已排队");
            RemoteSkillLocation location = locator.locate(request.source().toString());
            emit(stages, checkedListener, SkillInstallStage.DOWNLOADING, "正在下载 Skill");
            RemoteSkillPackage remote = fetcher.fetch(location, budget);
            budget.checkCancelled();

            Files.createDirectories(projectRoot);
            ensureSafeRoot();
            temporary = projectRoot.resolve(".install-" + UUID.randomUUID()).normalize();
            Files.createDirectory(temporary);
            writePackage(temporary, remote, budget);

            emit(stages, checkedListener, SkillInstallStage.VALIDATING, "正在验证 Skill");
            SkillDescriptor descriptor = parser.parseDescriptor(new FileSkillSource(temporary), SkillOrigin.PROJECT);
            LoadedSkill loaded = parser.parseLoaded(descriptor, "");
            String name = loaded.metadata().name();
            target = projectRoot.resolve(name).normalize();
            ensureChild(target);
            validateConflicts(loaded, request.force());

            boolean existed = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
            if (existed && !request.force()) throw new SkillInstallException("项目 Skill 已存在: " + name);
            emit(stages, checkedListener, SkillInstallStage.INSTALLING, "正在安装 Skill " + name);
            budget.checkCancelled();
            if (existed) {
                backup = projectRoot.resolve(".backup-" + name + "-" + UUID.randomUUID()).normalize();
                atomicMove(target, backup);
            }
            atomicMove(temporary, target);
            temporary = null;
            installed = true;

            emit(stages, checkedListener, SkillInstallStage.RELOADING, "正在刷新 Skill 目录");
            SkillCatalogSnapshot snapshot = refresher.reload();
            SkillDescriptor published = snapshot.find(name)
                    .filter(value -> value.origin() == SkillOrigin.PROJECT)
                    .orElseThrow(() -> new SkillInstallException("Skill 安装后未能发布"));
            if (!Path.of(published.source().id()).toAbsolutePath().normalize().startsWith(target)) {
                throw new SkillInstallException("Skill 安装后目录校验失败");
            }
            if (backup != null) {
                deleteTree(backup);
                backup = null;
            }
            emit(stages, checkedListener, SkillInstallStage.COMPLETED, "Skill 安装完成: " + name);
            return new SkillInstallResult(name, target, SkillOrigin.PROJECT, existed, stages);
        } catch (SkillInstallException exception) {
            rollback(target, backup, installed);
            throw exception;
        } catch (Exception exception) {
            rollback(target, backup, installed);
            throw new SkillInstallException("Skill 安装失败");
        } finally {
            if (temporary != null) deleteTree(temporary);
            activeBudget.compareAndSet(budget, null);
            activeThread.compareAndSet(Thread.currentThread(), null);
            busy.set(false);
        }
    }

    private void validateConflicts(LoadedSkill candidate, boolean force) {
        SkillCatalogSnapshot snapshot = loader.snapshot();
        String name = candidate.metadata().name();
        if (snapshot.find(name).isPresent() && !force) throw new SkillInstallException("Skill 已存在: " + name);
        Set<String> candidateCommands = new LinkedHashSet<>();
        candidateCommands.add(name);
        candidateCommands.addAll(candidate.metadata().aliases());
        for (SkillDescriptor existing : snapshot.skills().values()) {
            if (existing.metadata().name().equals(name)) continue;
            Set<String> existingCommands = new LinkedHashSet<>();
            existingCommands.add(existing.metadata().name());
            existingCommands.addAll(existing.metadata().aliases());
            if (candidateCommands.stream().anyMatch(existingCommands::contains)) {
                throw new SkillInstallException("Skill 命令名与现有 Skill 冲突");
            }
        }
    }

    private void writePackage(Path root, RemoteSkillPackage remote, SkillDownloadBudget budget) throws IOException {
        for (RemoteSkillFile file : remote.files()) {
            budget.checkCancelled();
            Path destination = root.resolve(file.relativePath()).normalize();
            if (!destination.startsWith(root)) throw new SkillInstallException("远程 Skill 文件路径逃逸");
            Path parent = destination.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.write(destination, file.content(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
    }

    private void rollback(Path target, Path backup, boolean installed) {
        try {
            if (installed && target != null) deleteTree(target);
            if (backup != null && Files.exists(backup, LinkOption.NOFOLLOW_LINKS)) atomicMove(backup, target);
            refresher.reload();
        } catch (Exception ignored) {
            // 不用内部异常覆盖原始安全错误。
        }
    }

    private void ensureSafeRoot() throws IOException {
        Path workspaceRoot = projectRoot.getParent().getParent().toRealPath();
        Path real = projectRoot.toRealPath();
        if (!real.startsWith(workspaceRoot) || Files.isSymbolicLink(projectRoot)) {
            throw new SkillInstallException("Skill 安装目录不安全");
        }
    }

    private void ensureChild(Path path) {
        if (!path.startsWith(projectRoot) || path.equals(projectRoot)) throw new SkillInstallException("Skill 安装路径越界");
    }

    private static void atomicMove(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new SkillInstallException("当前文件系统不支持原子安装");
        }
    }

    private void deleteTree(Path root) {
        if (root == null || !root.toAbsolutePath().normalize().startsWith(projectRoot)) return;
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    private static void emit(
            List<SkillInstallStage> stages,
            SkillInstallListener listener,
            SkillInstallStage stage,
            String message) {
        stages.add(stage);
        listener.onStage(stage, message);
    }

    @Override
    public void cancel() {
        SkillDownloadBudget budget = activeBudget.get();
        if (budget != null) budget.cancel();
        fetcher.cancel();
        Thread thread = activeThread.get();
        if (thread != null) thread.interrupt();
    }
}
