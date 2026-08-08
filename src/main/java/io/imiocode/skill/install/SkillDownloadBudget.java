package io.imiocode.skill.install;

import java.util.concurrent.atomic.AtomicBoolean;

/** 单次安装共享的文件数、字节数和取消预算。 */
public final class SkillDownloadBudget {
    private final SkillInstallConfig config;
    private int files;
    private long fileBytes;
    private long networkBytes;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public SkillDownloadBudget(SkillInstallConfig config) {
        this.config = java.util.Objects.requireNonNull(config, "config");
    }

    public synchronized void claimFile() {
        checkCancelled();
        if (++files > config.maxFiles()) throw new SkillInstallException("远程 Skill 文件数量超过限制");
    }

    public synchronized void consumeFileBytes(long count) {
        checkCancelled();
        if (count < 0) throw new IllegalArgumentException("字节数不能为负数");
        if (count > config.maxFileBytes()) throw new SkillInstallException("远程 Skill 单文件超过大小限制");
        if (fileBytes + count > config.maxTotalBytes()) throw new SkillInstallException("远程 Skill 总大小超过限制");
        fileBytes += count;
    }

    public synchronized void checkFileBytes(long count) {
        checkCancelled();
        if (count < 0) throw new IllegalArgumentException("字节数不能为负数");
        if (count > config.maxFileBytes()) throw new SkillInstallException("远程 Skill 单文件超过大小限制");
    }

    public synchronized void consumeResponseBytes(long count) {
        checkCancelled();
        if (count < 0) throw new IllegalArgumentException("字节数不能为负数");
        if (networkBytes + count > networkLimit()) throw new SkillInstallException("远程 Skill 下载响应超过限制");
        networkBytes += count;
    }

    public synchronized long remainingBytes() {
        return Math.max(0, networkLimit() - networkBytes);
    }

    public synchronized int files() { return files; }
    public synchronized long bytes() { return fileBytes; }
    public SkillInstallConfig config() { return config; }
    public void cancel() { cancelled.set(true); }
    public boolean cancelled() { return cancelled.get(); }

    public void checkCancelled() {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) {
            throw new SkillInstallException("Skill 安装已取消");
        }
    }

    private long networkLimit() {
        return config.maxTotalBytes() + Math.max(config.maxFileBytes(), 1024L * 1024);
    }
}
