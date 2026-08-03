package io.imiocode.memory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 人可读、按作用域隔离且原子替换的 Markdown 记忆存储。 */
public final class MarkdownMemoryStore implements MemoryStore {
    private static final Pattern ENTRY = Pattern.compile("^- \\[(m_[a-f0-9]{12})] \\[(preference|project_fact|decision)] (.+)$");
    private final Path userHome;
    private final Path userRoot;
    private final Path projectRoot;

    public MarkdownMemoryStore(Path userHome, Path projectRoot) {
        this.userHome = userHome.toAbsolutePath().normalize();
        this.userRoot = this.userHome.resolve(".imiocode");
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
    }

    @Override
    public synchronized MemoryDocument load(MemoryScope scope) {
        Path path = path(scope);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return MemoryDocument.empty(scope);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new MemoryException("记忆文件必须是普通文件");
        try {
            validateExistingPath(scope, path);
            List<MemoryEntry> entries = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
                Matcher matcher = ENTRY.matcher(line);
                if (!matcher.matches()) throw new MemoryException("记忆文件包含无法识别的条目");
                if (!ids.add(matcher.group(1))) throw new MemoryException("记忆文件包含重复 ID");
                MemoryCategory category = MemoryCategory.valueOf(matcher.group(2).toUpperCase(java.util.Locale.ROOT));
                entries.add(new MemoryEntry(matcher.group(1), category, matcher.group(3)));
            }
            return new MemoryDocument(scope, entries);
        } catch (IOException exception) { throw new MemoryException("无法读取记忆文件", exception); }
    }

    @Override
    public synchronized void replace(MemoryDocument document) {
        Path target = path(document.scope());
        Path parent = target.getParent();
        try {
            Files.createDirectories(parent);
            validateRoot(document.scope(), parent);
            StringBuilder text = new StringBuilder("# ImioCode Memories\n\n");
            document.entries().stream().sorted(Comparator.comparing(MemoryEntry::id)).forEach(entry -> text
                    .append("- [").append(entry.id()).append("] [")
                    .append(entry.category().name().toLowerCase(java.util.Locale.ROOT)).append("] ")
                    .append(entry.content()).append('\n'));
            Path temporary = Files.createTempFile(parent, ".memories-", ".tmp");
            try {
                try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                    ByteBuffer buffer = StandardCharsets.UTF_8.encode(text.toString());
                    while (buffer.hasRemaining()) channel.write(buffer);
                    channel.force(true);
                }
                validateRoot(document.scope(), parent);
                try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException exception) { throw new MemoryException("无法原子更新记忆文件", exception); }
    }

    public Path path(MemoryScope scope) {
        return scope == MemoryScope.USER ? userRoot.resolve("memories.md")
                : projectRoot.resolve(".imiocode").resolve("memories.md");
    }

    private void validateRoot(MemoryScope scope, Path parent) throws IOException {
        Path allowed = scope == MemoryScope.USER ? userHome : projectRoot;
        Files.createDirectories(allowed);
        if (!parent.toRealPath().startsWith(allowed.toRealPath())) throw new MemoryException("记忆路径超出允许范围");
    }

    private void validateExistingPath(MemoryScope scope, Path path) throws IOException {
        Path allowed = scope == MemoryScope.USER ? userHome : projectRoot;
        if (!path.toRealPath().startsWith(allowed.toRealPath())) {
            throw new MemoryException("记忆路径超出允许范围");
        }
    }
}
