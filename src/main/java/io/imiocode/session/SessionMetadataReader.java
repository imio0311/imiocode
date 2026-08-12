package io.imiocode.session;

import io.imiocode.session.record.SessionHeaderRecord;
import io.imiocode.session.record.SessionRecordCodec;
import io.imiocode.session.record.TransactionCommitRecord;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Optional;

/**
 * 从会话 JSONL 的头记录和最后一次提交快速提取列表元数据。
 *
 * <p>为避免列出大量会话时完整解析历史，只反向扫描文件末尾最多 1 MiB；找不到提交时回退到空会话元数据。</p>
 */
public final class SessionMetadataReader {
    private static final long MAX_TAIL_SCAN_BYTES = 1024L * 1024L;
    private final SessionRecordCodec codec;

    public SessionMetadataReader(SessionRecordCodec codec) { this.codec = codec; }

    public SessionSummary read(Path path) throws IOException {
        String firstLine;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            firstLine = reader.readLine();
        }
        if (firstLine == null || !(codec.decode(firstLine) instanceof SessionHeaderRecord header)) {
            throw new SessionException("会话文件缺少头记录");
        }
        if (header.schemaVersion() != 1) throw new SessionException("不支持的会话 schema");
        Instant updated = Instant.parse(header.createdAt());
        int count = 0;
        Optional<TransactionCommitRecord> lastCommit = findLastCommit(path);
        if (lastCommit.isPresent()) {
            TransactionCommitRecord commit = lastCommit.orElseThrow();
            updated = Instant.parse(commit.committedAt());
            count = commit.messageCount();
        }
        return new SessionSummary(new SessionId(header.sessionId()), Instant.parse(header.createdAt()), updated, count);
    }

    private Optional<TransactionCommitRecord> findLastCommit(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = channel.size();
            long minimum = Math.max(0, size - MAX_TAIL_SCAN_BYTES);
            int length = Math.toIntExact(size - minimum);
            ByteBuffer tail = ByteBuffer.allocate(length);
            channel.position(minimum);
            while (tail.hasRemaining() && channel.read(tail) >= 0) { }
            byte[] bytes = tail.array();
            ByteArrayOutputStream reversed = new ByteArrayOutputStream();
            // 从尾部逐字节寻找最近的有效提交，损坏或未提交的尾行会被跳过。
            for (int index = tail.position() - 1; index >= 0; index--) {
                byte value = bytes[index];
                if (value == '\n') {
                    Optional<TransactionCommitRecord> commit = decodeReversedLine(reversed);
                    if (commit.isPresent()) return commit;
                    reversed.reset();
                } else if (value != '\r') {
                    reversed.write(value);
                }
            }
            return decodeReversedLine(reversed);
        }
    }

    private Optional<TransactionCommitRecord> decodeReversedLine(ByteArrayOutputStream reversed) {
        byte[] bytes = reversed.toByteArray();
        if (bytes.length == 0) return Optional.empty();
        for (int left = 0, right = bytes.length - 1; left < right; left++, right--) {
            byte value = bytes[left]; bytes[left] = bytes[right]; bytes[right] = value;
        }
        String line = new String(bytes, StandardCharsets.UTF_8);
        if (!line.contains("\"type\":\"transaction_commit\"")) return Optional.empty();
        try {
            Object decoded = codec.decode(line);
            return decoded instanceof TransactionCommitRecord commit ? Optional.of(commit) : Optional.empty();
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
