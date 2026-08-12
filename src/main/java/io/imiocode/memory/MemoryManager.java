package io.imiocode.memory;

import io.imiocode.config.MemoryConfig;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 编排双作用域记忆的校验、去重、容量控制和原子替换。
 *
 * <p>公开修改方法使用同一实例锁串行化，避免读出旧文档后由并发写入覆盖；安全策略始终先于落盘执行。</p>
 */
public final class MemoryManager {
    private final MemoryStore store;
    private final MemorySafetyPolicy policy;
    private final MemoryConfig config;

    public MemoryManager(MemoryStore store, MemorySafetyPolicy policy, MemoryConfig config) {
        this.store = store; this.policy = policy; this.config = config;
    }

    public synchronized List<MemoryDocument> loadEnabledScopes() {
        if (!config.enabled()) return List.of();
        List<MemoryDocument> result = new ArrayList<>();
        if (config.userScopeEnabled()) result.add(store.load(MemoryScope.USER));
        if (config.projectScopeEnabled()) result.add(store.load(MemoryScope.PROJECT));
        return List.copyOf(result);
    }

    public synchronized MemoryEntry add(MemoryScope scope, String content) {
        MemoryCategory category = scope == MemoryScope.USER ? MemoryCategory.PREFERENCE : MemoryCategory.PROJECT_FACT;
        policy.validate(scope, category, content, false);
        MemoryDocument document = store.load(scope);
        ensureCapacity(document.entries().size() + 1);
        String normalized = normalizeForCompare(content);
        if (document.entries().stream().anyMatch(entry -> normalizeForCompare(entry.content()).equals(normalized))) {
            throw new MemoryException("相同记忆已存在");
        }
        MemoryEntry entry = MemoryEntry.create(category, content);
        List<MemoryEntry> updated = new ArrayList<>(document.entries()); updated.add(entry);
        replaceChecked(new MemoryDocument(scope, updated));
        return entry;
    }

    public synchronized MemoryEntry edit(MemoryScope scope, String id, String content) {
        MemoryDocument document = store.load(scope);
        int index = find(document, id);
        MemoryEntry old = document.entries().get(index);
        policy.validate(scope, old.category(), content, false);
        String normalized = normalizeForCompare(content);
        if (document.entries().stream()
                .anyMatch(entry -> !entry.id().equals(id)
                        && normalizeForCompare(entry.content()).equals(normalized))) {
            throw new MemoryException("相同记忆已存在");
        }
        List<MemoryEntry> updated = new ArrayList<>(document.entries());
        MemoryEntry replacement = new MemoryEntry(old.id(), old.category(), content);
        updated.set(index, replacement); replaceChecked(new MemoryDocument(scope, updated));
        return replacement;
    }

    public synchronized void forget(MemoryScope scope, String id) {
        MemoryDocument document = store.load(scope);
        int index = find(document, id);
        List<MemoryEntry> updated = new ArrayList<>(document.entries()); updated.remove(index);
        replaceChecked(new MemoryDocument(scope, updated));
    }

    public synchronized MemoryUpdateReport applyCandidates(List<MemoryCandidate> candidates) {
        int added = 0, updatedCount = 0, skipped = 0;
        List<String> warnings = new ArrayList<>();
        for (MemoryScope scope : MemoryScope.values()) {
            // 候选按作用域批量应用，每个作用域最多执行一次文档替换。
            if (!policy.scopeEnabled(scope)) continue;
            MemoryDocument document = store.load(scope);
            List<MemoryEntry> entries = new ArrayList<>(document.entries());
            boolean changed = false;
            for (MemoryCandidate candidate : candidates.stream().filter(item -> item.scope() == scope).toList()) {
                try {
                    policy.validate(scope, candidate.category(), candidate.content(), true);
                    String normalized = normalizeForCompare(candidate.content());
                    if (entries.stream().anyMatch(entry -> normalizeForCompare(entry.content()).equals(normalized))) {
                        skipped++; continue;
                    }
                    if (candidate.replacesId().isPresent()) {
                        int index = find(entries, candidate.replacesId().orElseThrow());
                        MemoryEntry old = entries.get(index);
                        entries.set(index, new MemoryEntry(old.id(), candidate.category(), candidate.content()));
                        updatedCount++; changed = true;
                    } else {
                        ensureCapacity(entries.size() + 1);
                        entries.add(MemoryEntry.create(candidate.category(), candidate.content()));
                        added++; changed = true;
                    }
                } catch (MemoryException exception) {
                    skipped++; warnings.add(exception.getMessage());
                }
            }
            if (changed) replaceChecked(new MemoryDocument(scope, entries));
        }
        return new MemoryUpdateReport(added, updatedCount, skipped, warnings);
    }

    public boolean enabled() { return config.enabled(); }
    public boolean autoExtractEnabled() { return config.enabled() && config.autoExtract(); }

    private void replaceChecked(MemoryDocument document) {
        // 同时校验条目数和最终 UTF-8 文件大小，不能用字符数近似持久化容量。
        ensureCapacity(document.entries().size());
        long bytes = renderBytes(document);
        if (bytes > config.maxFileBytes()) throw new MemoryException("记忆文件超过容量限制");
        store.replace(document);
    }

    private void ensureCapacity(int size) {
        if (size > config.maxEntriesPerScope()) throw new MemoryException("记忆条目数超过限制");
    }

    private static int find(MemoryDocument document, String id) { return find(document.entries(), id); }
    private static int find(List<MemoryEntry> entries, String id) {
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).id().equals(id)) return i;
        throw new MemoryException("未找到记忆 ID");
    }

    private static String normalizeForCompare(String value) {
        return MemoryEntry.normalizeLine(value).toLowerCase(Locale.ROOT);
    }

    private static long renderBytes(MemoryDocument document) {
        StringBuilder text = new StringBuilder("# ImioCode Memories\n\n");
        document.entries().stream().sorted(Comparator.comparing(MemoryEntry::id)).forEach(entry -> text
                .append("- [").append(entry.id()).append("] [")
                .append(entry.category().name().toLowerCase(Locale.ROOT)).append("] ")
                .append(entry.content()).append('\n'));
        return text.toString().getBytes(StandardCharsets.UTF_8).length;
    }
}
