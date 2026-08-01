package io.imiocode.context;

import io.imiocode.conversation.ToolResultPart;
import io.imiocode.tool.SecretRedactor;
import io.imiocode.tool.ToolResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** 将大工具结果安全、幂等地写入工作区内部目录。 */
public final class ToolResultSpillStore {
    private static final Path RELATIVE_DIRECTORY = Path.of(".imiocode", "tool-results");
    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");
    private final Path workspace;
    private final SecretRedactor redactor;

    public ToolResultSpillStore(Path workspace, SecretRedactor redactor) {
        this.workspace = Objects.requireNonNull(workspace, "workspace").toAbsolutePath().normalize();
        this.redactor = Objects.requireNonNull(redactor, "redactor");
        try {
            if (!Files.isDirectory(this.workspace, LinkOption.NOFOLLOW_LINKS) || isLinkLike(this.workspace)) {
                throw new IllegalArgumentException("工作区必须是存在的非链接目录");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法验证工作区安全性", exception);
        }
    }

    public SpilledResult spill(ToolResultPart part) throws IOException {
        Objects.requireNonNull(part, "part");
        String body = encode(part);
        String hash = sha256(body).substring(0, 16);
        String safeId = sanitize(part.callId());
        String fileName = safeId + "-" + hash + ".txt";
        Path directory = ensureDirectory();
        Path target = directory.resolve(fileName).normalize();
        verifyContained(target, directory);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            verifyRegular(target);
            String existing = Files.readString(target, StandardCharsets.UTF_8);
            if (!existing.equals(body)) throw new IOException("工具结果目标文件冲突");
            return new SpilledResult(RELATIVE_DIRECTORY.resolve(fileName), contentCharacters(part), false);
        }

        Path temporary = Files.createTempFile(directory, ".spill-", ".tmp");
        try {
            verifyRegular(temporary);
            Files.writeString(temporary, body, StandardCharsets.UTF_8);
            verifyDirectory(directory);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            } catch (java.nio.file.FileAlreadyExistsException exception) {
                verifyRegular(target);
                if (!Files.readString(target, StandardCharsets.UTF_8).equals(body)) throw exception;
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        verifyRegular(target);
        return new SpilledResult(RELATIVE_DIRECTORY.resolve(fileName), contentCharacters(part), true);
    }

    private Path ensureDirectory() throws IOException {
        Path metadata = workspace.resolve(".imiocode");
        createOrVerifyDirectory(metadata);
        Path directory = metadata.resolve("tool-results");
        createOrVerifyDirectory(directory);
        verifyContained(directory, workspace);
        return directory;
    }

    private static void createOrVerifyDirectory(Path path) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            verifyDirectory(path);
        } else {
            Files.createDirectory(path);
            verifyDirectory(path);
        }
    }

    private String encode(ToolResultPart part) {
        ToolResult result = part.result();
        return "tool_name: " + redactor.redact(part.toolName()) + "\n"
                + "call_id: " + redactor.redact(part.callId()) + "\n"
                + "success: " + result.success() + "\n"
                + "truncated: " + result.truncated() + "\n"
                + "exit_code: " + Objects.toString(result.exitCode(), "null") + "\n"
                + "duration: " + result.duration() + "\n"
                + "--- output ---\n" + redactor.redact(result.output()) + "\n"
                + "--- error ---\n" + redactor.redact(result.error()) + "\n";
    }

    private static int contentCharacters(ToolResultPart part) {
        return part.result().output().length() + part.result().error().length();
    }

    private static String sanitize(String callId) {
        String sanitized = Objects.requireNonNullElse(callId, "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-")
                .replaceAll("-+", "-").replaceAll("^[-_.]+|[-_.]+$", "");
        if (sanitized.length() > 48) sanitized = sanitized.substring(0, 48);
        if (sanitized.isBlank() || WINDOWS_RESERVED.contains(sanitized)) sanitized = "result";
        return sanitized;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境缺少 SHA-256", exception);
        }
    }

    private static void verifyContained(Path target, Path parent) throws IOException {
        Path normalized = target.toAbsolutePath().normalize();
        Path normalizedParent = parent.toAbsolutePath().normalize();
        if (!normalized.startsWith(normalizedParent) || normalized.equals(normalizedParent)) {
            throw new IOException("工具结果路径越界");
        }
    }

    private static void verifyDirectory(Path path) throws IOException {
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) || isLinkLike(path)) {
            throw new IOException("工具结果目录不安全");
        }
    }

    private static void verifyRegular(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || isLinkLike(path)) {
            throw new IOException("工具结果文件不安全");
        }
    }

    private static boolean isLinkLike(Path path) throws IOException {
        return Files.isSymbolicLink(path) || Files.readAttributes(path,
                java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isOther();
    }
}
