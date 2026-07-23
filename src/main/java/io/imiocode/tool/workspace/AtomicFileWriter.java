package io.imiocode.tool.workspace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** 使用同目录临时文件完成 UTF-8 文件创建或替换。 */
public final class AtomicFileWriter {
    private final WorkspacePolicy policy;

    public AtomicFileWriter(WorkspacePolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public void write(String input, String content) throws IOException {
        Path target = policy.resolveWritableFile(input);
        Path parent = target.getParent();
        Path temporary = Files.createTempFile(parent, ".imiocode-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            policy.revalidateWritable(target);
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                policy.revalidateWritable(target);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
