package io.imiocode.session;

import io.imiocode.conversation.ChatMessage;
import io.imiocode.session.record.MessageRecord;
import io.imiocode.session.record.SessionHeaderRecord;
import io.imiocode.session.record.SessionRecordCodec;
import io.imiocode.session.record.TransactionBeginRecord;
import io.imiocode.session.record.TransactionCommitRecord;
import io.imiocode.session.record.TransactionMode;

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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/** 项目级、仅追加、带提交摘要的 JSONL 会话存储。 */
public final class JsonlSessionStore implements SessionStore {
    private static final DateTimeFormatter QUARANTINE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);
    private final Path projectRoot;
    private final Path directory;
    private final Clock clock;
    private final SessionRecordCodec recordCodec;
    private final SessionMessageCodec messageCodec;
    private final SessionIntegrityValidator validator;
    private final SessionMetadataReader metadataReader;

    public JsonlSessionStore(Path projectRoot, Clock clock) {
        this(projectRoot, clock, new SessionRecordCodec(), new SessionMessageCodec());
    }

    public JsonlSessionStore(Path projectRoot, Clock clock,
                             SessionRecordCodec recordCodec, SessionMessageCodec messageCodec) {
        this.projectRoot = projectRoot.toAbsolutePath().normalize();
        this.directory = this.projectRoot.resolve(".imiocode").resolve("sessions");
        this.clock = clock;
        this.recordCodec = recordCodec;
        this.messageCodec = messageCodec;
        this.validator = new SessionIntegrityValidator();
        this.metadataReader = new SessionMetadataReader(recordCodec);
        ensureDirectory();
    }

    @Override
    public synchronized SessionSnapshot create() {
        SessionId id = SessionId.generate();
        Instant now = clock.instant();
        SessionMetadata metadata = new SessionMetadata(id, now, now, projectRoot.toString(), 0, 0);
        SessionHeaderRecord header = new SessionHeaderRecord(id.value(), projectRoot.toString(), now.toString());
        Path path = pathFor(id);
        writeNew(path, recordCodec.encode(header) + "\n");
        return new SessionSnapshot(metadata, List.of());
    }

    @Override
    public synchronized SessionSnapshot appendCommit(SessionSnapshot before, List<ChatMessage> afterHistory) {
        List<ChatMessage> after = List.copyOf(afterHistory);
        if (after.isEmpty()) throw new SessionException("不能提交空会话事务");
        messageCodec.validateChain(after);
        TransactionMode mode = startsWith(after, before.history()) ? TransactionMode.APPEND : TransactionMode.REPLACE;
        List<ChatMessage> written = mode == TransactionMode.APPEND
                ? after.subList(before.history().size(), after.size()) : after;
        if (written.isEmpty()) return before;

        String transactionId = UUID.randomUUID().toString().replace("-", "");
        TransactionBeginRecord begin = new TransactionBeginRecord(transactionId, mode, before.history().size());
        List<String> payload = new ArrayList<>();
        payload.add(recordCodec.encode(begin));
        for (int i = 0; i < written.size(); i++) {
            payload.add(recordCodec.encode(new MessageRecord(transactionId, i, messageCodec.encode(written.get(i)))));
        }
        long commitNumber = before.metadata().commitCount() + 1;
        Instant now = clock.instant();
        TransactionCommitRecord commit = new TransactionCommitRecord(transactionId, after.size(), commitNumber,
                recordCodec.transactionSha256(payload), now.toString());
        StringBuilder text = new StringBuilder();
        payload.forEach(line -> text.append(line).append('\n'));
        text.append(recordCodec.encode(commit)).append('\n');
        appendAndForce(pathFor(before.metadata().id()), text.toString());
        SessionMetadata metadata = new SessionMetadata(before.metadata().id(), before.metadata().createdAt(), now,
                before.metadata().workspaceIdentity(), commitNumber, after.size());
        return new SessionSnapshot(metadata, after);
    }

    @Override
    public synchronized SessionLoadResult load(SessionId id) {
        Path path = existingPath(id);
        String raw;
        try { raw = Files.readString(path, StandardCharsets.UTF_8); }
        catch (IOException exception) { throw new SessionException("无法读取会话文件", exception); }
        String[] all = raw.split("\n", -1);
        int lineCount = all.length;
        while (lineCount > 0 && all[lineCount - 1].isBlank()) lineCount--;
        if (lineCount == 0) throw new SessionException("会话文件为空");

        SessionHeaderRecord header;
        try {
            Object first = recordCodec.decode(all[0]);
            if (!(first instanceof SessionHeaderRecord value) || value.schemaVersion() != 1
                    || !id.value().equals(value.sessionId())) throw new SessionException("会话头记录无效");
            header = value;
        } catch (RuntimeException exception) {
            throw new SessionException("会话头记录损坏", exception);
        }

        List<ChatMessage> history = new ArrayList<>();
        int index = 1;
        int validLineCount = 1;
        long commitCount = 0;
        Instant updatedAt = Instant.parse(header.createdAt());
        try {
            while (index < lineCount) {
                int beginIndex = index;
                Object decoded = decodeAt(all, index);
                if (!(decoded instanceof TransactionBeginRecord begin)) {
                    throw new LogFailure("事务缺少 begin", index, beginIndex);
                }
                List<String> payload = new ArrayList<>();
                payload.add(all[index]);
                index++;
                List<MessageRecord> messages = new ArrayList<>();
                TransactionCommitRecord commit = null;
                while (index < lineCount) {
                    Object record = decodeAt(all, index);
                    if (record instanceof MessageRecord message) {
                        messages.add(message); payload.add(all[index]); index++; continue;
                    }
                    if (record instanceof TransactionCommitRecord value) {
                        commit = value; index++; break;
                    }
                    throw new LogFailure("事务记录顺序无效", index, beginIndex);
                }
                if (commit == null) throw new LogFailure("会话尾部事务未完成", index, beginIndex);
                try {
                    validator.validateTransaction(begin, messages, commit, history.size(), commitCount + 1,
                            recordCodec.transactionSha256(payload));
                    List<ChatMessage> decodedMessages = messages.stream()
                            .map(item -> messageCodec.decode(item.message())).toList();
                    if (begin.mode() == TransactionMode.REPLACE) history.clear();
                    history.addAll(decodedMessages);
                    messageCodec.validateChain(history);
                } catch (RuntimeException exception) {
                    throw new LogFailure(exception.getMessage(), index - 1, beginIndex, exception);
                }
                commitCount = commit.commitNumber();
                updatedAt = Instant.parse(commit.committedAt());
                validLineCount = index;
            }
        } catch (LogFailure failure) {
            if (commitCount == 0 || !isTailFailure(all, lineCount, failure.index)) {
                throw new SessionException("会话中部损坏，已拒绝恢复", failure);
            }
            Path quarantine = quarantineAndRepair(path, all, validLineCount, lineCount);
            SessionSnapshot snapshot = snapshot(header, updatedAt, commitCount, history);
            return new SessionLoadResult(snapshot, SessionRecoveryStatus.TAIL_RECOVERED,
                    Optional.of(quarantine), Optional.of("会话尾部损坏，已恢复到最后完整提交"));
        }
        return SessionLoadResult.clean(snapshot(header, updatedAt, commitCount, history));
    }

    @Override
    public synchronized List<SessionSummary> list() {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(path -> path.getFileName().toString().endsWith(".jsonl"))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .map(path -> {
                        try { return metadataReader.read(path); } catch (RuntimeException | IOException ignored) { return null; }
                    })
                    .filter(java.util.Objects::nonNull)
                    .sorted(Comparator.comparing(SessionSummary::updatedAt).reversed())
                    .toList();
        } catch (IOException exception) {
            throw new SessionException("无法列出项目会话", exception);
        }
    }

    @Override
    public synchronized void delete(SessionId id) {
        Path path = existingPath(id);
        try { Files.delete(path); } catch (IOException exception) { throw new SessionException("无法删除会话", exception); }
    }

    private SessionSnapshot snapshot(SessionHeaderRecord header, Instant updatedAt,
                                     long commitCount, List<ChatMessage> history) {
        SessionMetadata metadata = new SessionMetadata(new SessionId(header.sessionId()), Instant.parse(header.createdAt()),
                updatedAt, header.workspaceIdentity(), commitCount, history.size());
        return new SessionSnapshot(metadata, history);
    }

    private Object decodeAt(String[] lines, int index) {
        try { return recordCodec.decode(lines[index]); }
        catch (RuntimeException exception) { throw new LogFailure("会话 JSONL 记录损坏", index, index, exception); }
    }

    private static boolean isTailFailure(String[] lines, int lineCount, int failureIndex) {
        if (failureIndex >= lineCount - 1) return true;
        for (int i = Math.max(0, failureIndex + 1); i < lineCount; i++) {
            if (lines[i].contains("\"type\":\"transaction_commit\"")) return false;
        }
        return true;
    }

    private Path quarantineAndRepair(Path path, String[] lines, int validLineCount, int lineCount) {
        String suffix = joinLines(lines, validLineCount, lineCount);
        String prefix = joinLines(lines, 0, validLineCount);
        Path quarantine = directory.resolve(path.getFileName() + ".corrupt-" + QUARANTINE_TIME.format(clock.instant()));
        writeNew(quarantine, suffix);
        atomicReplace(path, prefix);
        return quarantine;
    }

    private static String joinLines(String[] lines, int start, int end) {
        StringBuilder text = new StringBuilder();
        for (int i = start; i < end; i++) text.append(lines[i]).append('\n');
        return text.toString();
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(directory);
            Path projectReal = projectRoot.toRealPath();
            if (!directory.toRealPath().startsWith(projectReal)) throw new SessionException("会话目录超出项目范围");
        } catch (IOException exception) { throw new SessionException("无法创建会话目录", exception); }
    }

    private Path pathFor(SessionId id) { return directory.resolve(id.value() + ".jsonl").normalize(); }

    private Path existingPath(SessionId id) {
        Path path = pathFor(id);
        if (!path.getParent().equals(directory) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new SessionException("会话不存在");
        }
        return path;
    }

    private void writeNew(Path path, String content) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            writeAll(channel, content); channel.force(true);
        } catch (IOException exception) { throw new SessionException("无法创建会话文件", exception); }
    }

    private void appendAndForce(Path path, String content) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new SessionException("会话文件不存在");
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
             var ignored = channel.lock()) {
            writeAll(channel, content); channel.force(true);
        } catch (IOException exception) { throw new SessionException("无法提交会话事务", exception); }
    }

    private void atomicReplace(Path target, String content) {
        Path temporary;
        try { temporary = Files.createTempFile(directory, ".session-repair-", ".tmp"); }
        catch (IOException exception) { throw new SessionException("无法创建会话修复临时文件", exception); }
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) { writeAll(channel, content); channel.force(true); }
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) { throw new SessionException("无法修复会话文件", exception); }
        finally { try { Files.deleteIfExists(temporary); } catch (IOException ignored) { } }
    }

    private static void writeAll(FileChannel channel, String content) throws IOException {
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(content);
        while (buffer.hasRemaining()) channel.write(buffer);
    }

    private static boolean startsWith(List<ChatMessage> after, List<ChatMessage> before) {
        return after.size() >= before.size() && after.subList(0, before.size()).equals(before);
    }

    private static final class LogFailure extends SessionException {
        private final int index;
        private final int beginIndex;
        private LogFailure(String message, int index, int beginIndex) { super(message); this.index = index; this.beginIndex = beginIndex; }
        private LogFailure(String message, int index, int beginIndex, Throwable cause) { super(message, cause); this.index = index; this.beginIndex = beginIndex; }
    }
}
