package io.imiocode.worktree.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.imiocode.worktree.WorktreeException;
import io.imiocode.worktree.model.WorktreeSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

/** `.imiocode/worktree-session.json` 的原子存储。 */
public final class WorktreeSessionStore {
    public static final String FILE_NAME = "worktree-session.json";
    private final Path repositoryRoot;
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();

    public WorktreeSessionStore(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot.toAbsolutePath().normalize();
        this.file = this.repositoryRoot.resolve(".imiocode").resolve(FILE_NAME);
    }

    public synchronized void save(WorktreeSession session) {
        try {
            Files.createDirectories(file.getParent());
            ObjectNode root = mapper.createObjectNode();
            root.put("schemaVersion", 1);
            root.put("sessionId", session.sessionId()); root.put("slug", session.slug());
            root.put("originalCwd", session.originalCwd().toString());
            root.put("worktreePath", session.worktreePath().toString());
            root.put("worktreeBranch", session.worktreeBranch());
            root.put("originalBranch", session.originalBranch());
            root.put("originalHead", session.originalHead());
            root.put("createdAt", session.createdAt().toString());
            Path temporary = Files.createTempFile(file.getParent(), ".worktree-session-", ".tmp");
            try {
                Files.writeString(temporary, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n",
                        StandardCharsets.UTF_8);
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException exception) { throw new WorktreeException("无法保存 Worktree 会话", exception); }
    }

    public synchronized Optional<WorktreeSession> load() {
        if (!Files.exists(file)) return Optional.empty();
        if (!Files.isRegularFile(file)) throw new WorktreeException("Worktree 会话记录不是普通文件");
        try {
            JsonNode root = mapper.readTree(Files.readString(file, StandardCharsets.UTF_8));
            if (root == null || root.path("schemaVersion").asInt(-1) != 1) {
                throw new WorktreeException("Worktree 会话记录版本无效");
            }
            return Optional.of(new WorktreeSession(text(root, "sessionId"), text(root, "slug"),
                    Path.of(text(root, "originalCwd")), Path.of(text(root, "worktreePath")),
                    text(root, "worktreeBranch"), text(root, "originalBranch"),
                    text(root, "originalHead"), Instant.parse(text(root, "createdAt"))));
        } catch (WorktreeException exception) { throw exception; }
        catch (RuntimeException | IOException exception) {
            throw new WorktreeException("Worktree 会话记录损坏", exception);
        }
    }

    public synchronized void clear() {
        try { Files.deleteIfExists(file); }
        catch (IOException exception) { throw new WorktreeException("无法清除 Worktree 会话", exception); }
    }

    public boolean exists() { return Files.isRegularFile(file); }
    public Path file() { return file; }
    public Path repositoryRoot() { return repositoryRoot; }

    private static String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new WorktreeException("Worktree 会话记录缺少字段: " + field);
        }
        return value.textValue();
    }
}
